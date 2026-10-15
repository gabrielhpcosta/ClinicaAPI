package com.clinica.api.services;

import java.time.DayOfWeek;

import com.clinica.api.config.mappers.ConsultaMapper;
import com.clinica.api.dto.response.DisponibilidadeResponse;
import com.clinica.api.exception.BadRequestException;
import com.clinica.api.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.clinica.api.dto.request.CancelarConsultaRequest;
import com.clinica.api.dto.request.ConsultaRequest;
import com.clinica.api.dto.response.ConsultaResponse;
import com.clinica.api.entities.Consulta;
import com.clinica.api.entities.Medico;
import com.clinica.api.entities.Paciente;
import com.clinica.api.enums.StatusConsulta;
import com.clinica.api.exception.NotFound;
import com.clinica.api.repositories.ConsultaRepository;
import com.clinica.api.repositories.MedicoRepository;
import com.clinica.api.repositories.PacienteRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final ConsultaMapper consultaMapper;
    private final FeriadoService feriadoService;

    public ConsultaService(ConsultaRepository consultaRepository, MedicoRepository medicoRepository, PacienteRepository pacienteRepository, ConsultaMapper consultaMapper, FeriadoService feriadoService) {
        this.consultaRepository = consultaRepository;
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.consultaMapper = consultaMapper;
        this.feriadoService = feriadoService;
    }

    private static final Logger log = LoggerFactory.getLogger(ConsultaService.class);

    @Cacheable("consultas")
    public List<ConsultaResponse> buscarTodos() {
        log.info("Buscando todas as consultas");
        return consultaRepository.findAll().stream().map(consultaMapper::toResponse).toList();
    }

    @Cacheable(value = "consulta", key = "#id")
    public ConsultaResponse buscarPorId(Long id) {
        log.info("Buscando consulta com ID {}", id);
        Consulta consulta = consultaRepository.findById(id).orElseThrow(() -> new NotFound("Consulta não encontrada"));

        return consultaMapper.toResponse(consulta);
    }

    @Cacheable(value = "disponibilidade", key = "#medicoId + '-' + #data")
    public DisponibilidadeResponse buscarDisponibilidade(Long medicoId, LocalDate data) {
        log.info("Buscando disponibilidade do médico {} para {}", medicoId, data);

        Medico medico = medicoRepository.findById(medicoId).orElseThrow(() -> new NotFound("Médico não encontrado"));

        if (!medico.getAtivo()) {
            throw new BadRequestException("Não é possivel consultar a disponibilidade de um médico");
        }

        DayOfWeek diaSemana = data.getDayOfWeek();

        if (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) {
            throw new BadRequestException("O médico não atende aos finais de semana");
        }

        if (feriadoService.ehFeriado(data)) {
            throw new BadRequestException("O médico não atende durante feriados");
        }

        List<DisponibilidadeResponse.HorarioResponse> horarios = new ArrayList<>();

        LocalDateTime horarioAtual = data.atTime(7, 0);
        LocalDateTime fimExpediente = data.atTime(19, 0);

        while (horarioAtual.isBefore(fimExpediente)) {

            LocalDateTime inicio = horarioAtual;
            LocalDateTime fim = horarioAtual.plusMinutes(30);

            boolean ocupado = consultaRepository.existsByMedicoAndStatusAndDataHoraBetween(medico, StatusConsulta.AGENDADA, inicio, fim.minusNanos(1));

            // Só adiciona o horário se estiver livre
            if (!ocupado) {
                horarios.add(new DisponibilidadeResponse.HorarioResponse(horarioAtual.toLocalTime().toString(), true));
            }

            horarioAtual = horarioAtual.plusMinutes(30);
        }

        return new DisponibilidadeResponse(medico.getId(), medico.getNome(), data, horarios);
    }

    @CacheEvict(value = {"consultas", "consulta", "disponibilidade"}, allEntries = true)
    public ConsultaResponse salvar(ConsultaRequest request) {
        log.info("Cadastrando nova consulta para o médico {} e paciente {}", request.getMedicoId(), request.getPacienteId());
        Medico medico = medicoRepository.findById(request.getMedicoId()).orElseThrow(() -> new NotFound("Médico não encontrado"));

        Paciente paciente = pacienteRepository.findById(request.getPacienteId()).orElseThrow(() -> new NotFound("Paciente não encontrado"));

        if (!medico.getAtivo()) {
            throw new BadRequestException("Não é possível agendar consulta para um médico inativo");
        }

        if (!paciente.getAtivo()) {
            throw new BadRequestException(("Não é possivel agendar uma consulta com o paciente inativo"));
        }

        if (request.getDataHora().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Não é possível agendar uma consulta em uma data/horário no passado");
        }

        int hora = request.getDataHora().getHour();
        if (hora < 7 || hora >= 19) {
            throw new BadRequestException("Horário indisponivel para Agendamento");
        }

        DayOfWeek diaSemana = request.getDataHora().getDayOfWeek();
        if (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) {
            throw new BadRequestException("Dia indisponivel para Agendamento");
        }

        //manda uma requisição para verificar se o dia que está tentando cadastrar a consulta é feriado ou não
        if (feriadoService.ehFeriado(request.getDataHora().toLocalDate())) {
            throw new BadRequestException("Não é possivel agendar uma consulta em um feriado");
        }

        LocalDateTime dataHora = request.getDataHora();

        boolean medicoOcupado = consultaRepository.existsByMedicoAndStatusAndDataHoraBetween(medico, StatusConsulta.AGENDADA, dataHora.minusMinutes(30).plusNanos(1), dataHora.plusMinutes(30).minusNanos(1));

        if (medicoOcupado) {
            throw new ConflictException("O médico já possui uma consulta neste horário");
        }

        LocalDateTime inicioDoDia = request.getDataHora().toLocalDate().atStartOfDay();
        LocalDateTime finalDoDia = request.getDataHora().toLocalDate().atTime(23, 59, 59);

        if (consultaRepository.existsByPacienteAndStatusAndDataHoraBetween(paciente, StatusConsulta.AGENDADA, inicioDoDia, finalDoDia)) {
            throw new ConflictException("O paciente já possui uma consulta Agendada neste dia");
        }

        Consulta consulta = consultaMapper.toEntity(request);
        consulta.setMedico(medico);
        consulta.setPaciente(paciente);
        consulta.setStatus(StatusConsulta.AGENDADA);

        Consulta consultaSalva = consultaRepository.save(consulta);
        return consultaMapper.toResponse(consultaSalva);
    }

    @CacheEvict(value = {"consultas", "consulta", "disponibilidade"}, allEntries = true)
    public ConsultaResponse cancelar(Long id, CancelarConsultaRequest request) {
        log.info("Cancelando consulta com ID {}", id);
        Consulta consulta = consultaRepository.findById(id).orElseThrow(() -> new NotFound("Consulta não encontrada"));

        if (consulta.getStatus() == StatusConsulta.CANCELADA) {
            throw new BadRequestException("A consulta já está cancelada");
        }

        LocalDateTime agora = LocalDateTime.now();
        long horas = Duration.between(agora, consulta.getDataHora()).toHours();

        if (horas < 24) {
            throw new BadRequestException("A consulta so pode ser cancelada com 24 horas de antecedencia");
        }

        consulta.setStatus(StatusConsulta.CANCELADA);
        consulta.setMotivoCancelamento(request.getMotivo());

        return consultaMapper.toResponse(consultaRepository.save(consulta));

    }

}
