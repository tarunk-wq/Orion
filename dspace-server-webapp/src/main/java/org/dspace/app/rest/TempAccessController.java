package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.TempAccessConverter;
import org.dspace.app.rest.model.TempAccessDTO;
import org.dspace.app.rest.model.TempAccessResponse;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.services.ConfigurationService;
import org.dspace.tempaccess.TempAccess;
import org.dspace.tempaccess.service.TempAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/core/tempaccess")
public class TempAccessController {

	private static final Logger log = LogManager.getLogger(TempAccessController.class);

	@Autowired
	private TempAccessService tempAccessService;

	@Autowired
	private TempAccessConverter tempAccessConverter;

	@Autowired
	private ConfigurationService configurationService;

	@PostMapping("/create")
	public TempAccessResponse create(@RequestBody ObjectNode tempAccessJson, HttpServletRequest request)
			throws Exception {
		Context context = ContextUtil.obtainContext(request);
		try {
			String frontendBaseUrl = configurationService.getProperty("mail.base.url");
			TempAccess tempAccess = tempAccessService.create(context, tempAccessJson);
			TempAccessDTO dto = tempAccessConverter.convert(tempAccess);

			UUID itemUuid = tempAccess.getItem().getID();
			String accessUrl = frontendBaseUrl + "/bitstreams/view?print=true" + "&item=" + itemUuid;

			try {
				sendApprovalEmail(context, tempAccess.getEperson().getEmail(), accessUrl, tempAccess.getStartDate(),
						tempAccess.getEndDate());
			} catch (Exception e) {
				log.error("Failed to send approval email to " + tempAccess.getEperson().getEmail(), e);
			}
			context.complete();
			return new TempAccessResponse(dto, accessUrl);
		} catch (Exception e) {
			if (context != null && context.isValid())
				context.abort();
			log.error("Error in creating TimeAccess", e);
			throw e;
		}
	}

	@GetMapping("/findbyuser")
	public ResponseEntity<TempAccessDTO> findByItemAndUser(@RequestParam UUID itemUuid, @RequestParam String userEmail,
			HttpServletRequest request) throws SQLException {
		Context context = ContextUtil.obtainContext(request);
		try {
			TempAccess ta = tempAccessService.findByItemAndUser(context, itemUuid, userEmail);

			if (ta == null) {
				context.complete();
				return ResponseEntity.notFound().build();
			}
			TempAccessDTO dto = tempAccessConverter.convert(ta);
			context.complete();

			return ResponseEntity.ok(dto);
		} catch (Exception e) {
			if (context != null && context.isValid())
				context.abort();
			log.error("Error in finding TempAccess: itemUuid=" + itemUuid + ", userEmail=" + userEmail, e);
			throw e;
		}
	}

	@DeleteMapping("/deletebyuser")
	public void deleteByItemAndUser(@RequestParam UUID itemUuid, @RequestParam String userEmail,
			HttpServletRequest request) throws SQLException {
		Context context = ContextUtil.obtainContext(request);
		try {
			tempAccessService.deleteByItemAndUser(context, itemUuid, userEmail);
			context.complete();
		} catch (Exception e) {
			if (context != null && context.isValid())
				context.abort();
			log.error("Error in deleting TempAccess: itemUuid=" + itemUuid + ", userEmail=" + userEmail, e);
			throw e;
		}
	}

	@GetMapping("/{uuid}/item")
	public Page<TempAccessDTO> getByItem(@PathVariable UUID uuid, Pageable pageable, HttpServletRequest request)
			throws SQLException {
		Context context = ContextUtil.obtainContext(request);
		try {
			int limit = pageable.getPageSize() > 0 ? pageable.getPageSize() : 10;
			int pageNumber = pageable.getPageNumber() >= 0 ? pageable.getPageNumber() : 0;
			int offset = pageNumber * limit;
			List<TempAccessDTO> result = tempAccessService.findByItem(context, uuid, limit, offset).stream()
					.map(tempAccessConverter::convert).collect(Collectors.toList());
			long total = tempAccessService.countByItem(context, uuid);
			context.complete();
			return new PageImpl<>(result, pageable, total);
		} catch (Exception e) {
			if (context != null && context.isValid())
				context.abort();
			log.error("Error in fetching paginated TempAccess list by item: itemUuid=" + uuid, e);
			throw e;
		}
	}

	private void sendApprovalEmail(Context context, String recipientEmail, String accessUrl, LocalDate startDate,
			LocalDate endDate) throws IOException, MessagingException {
		Locale locale = context.getCurrentLocale();
		Email emailBean = Email.getEmail(I18nUtil.getEmailFilename(locale, "temp_access"));

		String formattedStart = startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
		String formattedEnd = endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

		emailBean.addRecipient(recipientEmail);
		emailBean.addArgument(accessUrl);
		emailBean.addArgument(formattedStart);
		emailBean.addArgument(formattedEnd);
		emailBean.send();

		log.info("Approval email sent to: " + recipientEmail);
	}
}