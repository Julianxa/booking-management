package com.example.service;

import com.example.constant.Enums;
import com.example.exception.BusinessException;
import com.example.exception.report.ReportNotFoundException;
import com.example.exception.ErrorCode;
import com.example.mapper.ReportMapper;
import com.example.model.dto.DeleteReportResponseDTO;
import com.example.model.dto.GenerateReportRequestDTO;
import com.example.model.dto.GenerateReportResponseDTO;
import com.example.model.dto.GetListReportsResponseDTO;
import com.example.model.dto.ReportSummaryResponseDTO;
import com.example.model.entity.Reports;
import com.example.repository.ReportsRepository;
import com.example.utils.ReferenceNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
  private final ReportsRepository reportsRepository;
  private final AwsService awsService;
  private final ReferenceNoGenerator referenceNoGenerator;
  private final ReportMapper reportMapper;
  private final ReportGenerationAsyncService reportGenerationAsyncService;

  @Transactional
  public GenerateReportResponseDTO generateReport(GenerateReportRequestDTO request) {
    Reports report = createQueuedReport(request);
    reportGenerationAsyncService.generateReportAsync(report.getId());
    return toQueuedResponse(report, "Report generation started");
  }

  @Transactional(readOnly = true)
  public GetListReportsResponseDTO getAllReports(
      Pageable pageable, Enums.ReportType reportType) {
    Page<Reports> reportsPage =
        reportType == null
            ? reportsRepository.findAll(pageable)
            : reportsRepository.findByReportType(reportType, pageable);

    List<ReportSummaryResponseDTO> content =
        reportsPage.getContent().stream().map(this::toSummaryWithDownloadUrl).toList();

    Page<ReportSummaryResponseDTO> mappedPage =
        new PageImpl<>(content, pageable, reportsPage.getTotalElements());
    return reportMapper.toGetListResponse(mappedPage);
  }

  @Transactional(readOnly = true)
  public ReportSummaryResponseDTO getReportByRefNo(String refNo) {
    Reports report =
        reportsRepository
            .findByRefNo(refNo)
            .orElseThrow(
                () -> new ReportNotFoundException(String.format("Report %s not found", refNo)));
    return toSummaryWithDownloadUrl(report);
  }

  @Transactional
  public DeleteReportResponseDTO deleteReportByRefNo(String refNo) {
    Reports report =
        reportsRepository
            .findByRefNo(refNo)
            .orElseThrow(
                () -> new ReportNotFoundException(String.format("Report %s not found", refNo)));

    if (report.getS3Key() != null && !report.getS3Key().isBlank()) {
      awsService.deleteFile(report.getS3Key());
    }

    reportsRepository.delete(report);

    DeleteReportResponseDTO response = new DeleteReportResponseDTO();
    response.setMessage("Report deleted successfully");
    response.setTimestamp(ZonedDateTime.now());
    return response;
  }

  private ReportSummaryResponseDTO toSummaryWithDownloadUrl(Reports report) {
    ReportSummaryResponseDTO summary = reportMapper.toSummaryResponseDTO(report);
    boolean completedWithFile =
        report.getStatus() == Enums.ReportStatus.COMPLETED
            && report.getS3Key() != null
            && !report.getS3Key().isBlank();
    if (completedWithFile) {
      summary.setDownloadUrl(awsService.getFileFromS3(report.getS3Key(), Duration.ofHours(1)));
    }
    return summary;
  }

  private Reports createQueuedReport(GenerateReportRequestDTO request) {
    LocalDate startDate = request.getStartDate();
    LocalDate endDate = request.getEndDate();

    if (startDate.isAfter(endDate)) {
      throw new BusinessException(
          ErrorCode.MISSING_REQUIRED_FIELD, "start_date must be on or before end_date");
    }

    return reportsRepository.save(
        Reports.builder()
            .refNo(referenceNoGenerator.generateReportReference())
            .reportType(request.getReportType())
            .status(Enums.ReportStatus.PENDING)
            .startDate(startDate)
            .endDate(endDate)
            .generatedBy(request.getGeneratedBy())
            .includedBookingEvents(0)
            .totalBookingEventsInRange(0L)
            .build());
  }

  private GenerateReportResponseDTO toQueuedResponse(Reports report, String message) {
    return GenerateReportResponseDTO.builder()
        .id(report.getRefNo())
        .reportType(report.getReportType())
        .reportStartDate(report.getStartDate())
        .reportEndDate(report.getEndDate())
        .includedBookingEvents(report.getIncludedBookingEvents())
        .totalBookingEventsInRange(report.getTotalBookingEventsInRange())
        .status(report.getStatus())
        .errorMessage(report.getErrorMessage())
        .message(message)
        .timestamp(ZonedDateTime.now())
        .build();
  }
}
