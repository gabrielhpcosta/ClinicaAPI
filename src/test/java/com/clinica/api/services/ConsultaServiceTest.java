package com.clinica.api.services;

import com.clinica.api.config.mappers.ConsultaMapper;
import com.clinica.api.dto.request.ConsultaRequest;
import com.clinica.api.entities.Consulta;
import com.clinica.api.entities.Medico;
import com.clinica.api.entities.Paciente;
import com.clinica.api.enums.StatusConsulta;
import com.clinica.api.exception.BadRequestException;
import com.clinica.api.repositories.ConsultaRepository;
import com.clinica.api.repositories.MedicoRepository;
import com.clinica.api.repositories.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConsultaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO = Clock.fixed(
            LocalDateTime.of(2026,10,14,9,0).atZone(ZONA).toInstant(), ZONA
    );

    private static final LocalDateTime DATA_VALIDA = LocalDateTime.of(2026,10,21,10,0);

    @Mock private ConsultaRepository consultaRepository;
    @Mock private MedicoRepository medicoRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private ConsultaMapper consultaMapper;
    @Mock private FeriadoService feriadoService;

    private ConsultaService service;

    @BeforeEach
    void setUp() {
        service = new ConsultaService(consultaRepository, medicoRepository, pacienteRepository, consultaMapper, feriadoService, RELOGIO);
    }

    private Medico medico(boolean ativo) {
        Medico m = new Medico();
        m.setId(1L);
        m.setAtivo(ativo);
        return m;
    }

    private Paciente paciente(boolean ativo) {
        Paciente p = new Paciente();
        p.setId(2L);
        p.setAtivo(ativo);
        return p;
    }

    private ConsultaRequest request(LocalDateTime dataHora) {
        ConsultaRequest r = new ConsultaRequest();
        r.setMedicoId(1L);
        r.setPacienteId(2L);
        r.setDataHora(dataHora);
        return r;
    }

    @Test
    void salvar_deveAgendarConsulta_quandoTudoValido() {
        ConsultaRequest req = request(DATA_VALIDA);
        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico(true)));
        when(pacienteRepository.findById(2L)).thenReturn(Optional.of(paciente(true)));
        when(feriadoService.ehFeriado(any())).thenReturn(false);
        when(consultaMapper.toEntity(req)).thenReturn(new Consulta());
        when(consultaRepository.save(any(Consulta.class))).thenAnswer(inv -> inv.getArgument(0));

        service.salvar(req);

        ArgumentCaptor<Consulta> captor = ArgumentCaptor.forClass(Consulta.class);
        verify(consultaRepository).save(captor.capture());
        Consulta salva = captor.getValue();
        assertThat(salva.getStatus()).isEqualTo(StatusConsulta.AGENDADA);
        assertThat(salva.getMedico().getId()).isEqualTo(1L);
        assertThat(salva.getPaciente().getId()).isEqualTo(2L);
    }

    @Test
    void salvar_deveLancarErro_quandoMedicoInativo() {
        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico(false)));
        when(pacienteRepository.findById(2L)).thenReturn(Optional.of(paciente(true)));

        assertThatThrownBy(() -> service.salvar(request(DATA_VALIDA)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("médico inativo");

        verify(consultaRepository, never()).save(any());
    }
}
