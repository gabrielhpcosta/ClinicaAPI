package com.clinica.api.repositories;

import com.clinica.api.entities.Consulta;
import com.clinica.api.entities.Medico;
import com.clinica.api.entities.Paciente;
import com.clinica.api.enums.StatusConsulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    boolean existsByPacienteAndStatusAndDataHoraBetween(Paciente paciente, StatusConsulta status, LocalDateTime inicio, LocalDateTime fim);
    boolean existsByMedicoAndStatusAndDataHoraBetween(Medico medico, StatusConsulta status, LocalDateTime inicio, LocalDateTime fim);
}
