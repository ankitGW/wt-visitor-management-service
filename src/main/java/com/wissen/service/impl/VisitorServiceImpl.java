package com.wissen.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.wissen.constants.Constants;
import com.wissen.decorator.VisitorDecorator;
import com.wissen.dto.FilterRequest;
import com.wissen.dto.VisitorDto;
import com.wissen.enrich.FilterResult;
import com.wissen.enrich.FilterSpecification;
import com.wissen.entity.Employee;
import com.wissen.entity.Timing;
import com.wissen.entity.Visitor;
import com.wissen.exceptions.VisitorManagementException;
import com.wissen.repository.VisitorRepository;
import com.wissen.service.TimingService;
import com.wissen.service.VisitorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Implementation class for visitor service.
 *
 * @author Vishal Tomar
 */
@Service
@Slf4j
public class VisitorServiceImpl implements VisitorService {

    @Autowired
    private VisitorRepository visitorRepository;

    @Autowired
    private VisitorDecorator visitorDecorator;

    @Autowired
    private TimingService timingService;
    
    @Autowired
    EmailService emailService;

    /**
     * Filter Specification is used to enrich the input request
     * This class will form dynamic queries
     * No dao layer is been called in this class, just a transformer
     */
    @Autowired
    FilterSpecification<Visitor> filterSpecification;

    /**
     * Save visitor details.
     *
     * @param visitorDto
     * @return savedVisitor
     */
    @Override
    @Transactional
    public Visitor saveVisitorDetails(VisitorDto visitorDto) {

        // decorating before saving
        Visitor visitor = visitorDecorator.decorateBeforeSaving(visitorDto);

        // if user already checked in
        List<Timing> timings = this.timingService.findByVisitorAndOutTime(visitor, null);
        if(!CollectionUtils.isEmpty(timings))
            throw new VisitorManagementException("Visitor already checked in.");

        //save/update the details
        Visitor savedVisitor = this.visitorRepository.save(visitor);
        sendEmailToHost(savedVisitor);

        // decorating after saving
        visitorDecorator.decorateAfterSaving(savedVisitor, null);

        return savedVisitor;
    }

    /**
     * Method will save or update the details based on the details provided
     *
     * @param outDetails
     * @return List of saved or updated details
     */
    @Override
    public List<Visitor> saveOrUpdateVisitors(List<Visitor> outDetails) {
        return visitorRepository.saveAll(outDetails);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public List<Visitor> getVisitorsByPhoneNoOrEmail(String phNo, String email) {
        return this.visitorRepository.findByPhoneNumberOrEmail(phNo, email);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public List<Visitor> getVisitorByFilter(List<FilterRequest> requestFilters) {

        List<Visitor> visitors = new ArrayList<>();
        if (CollectionUtils.isEmpty(requestFilters)) {
            //TODO fetch only 30days records pass in time in the request
            visitors.addAll(visitorRepository.findAll());
        } else {
            Specification<Visitor> specificationRequest = filterSpecification.getSpecificationFromFilters(requestFilters);
            visitors.addAll(Sets.newHashSet(visitorRepository.findAll(specificationRequest)));
            visitors = FilterResult.filterExtraByFilterRequest(requestFilters, visitors);
        }

        if(CollectionUtils.isNotEmpty(visitors)) {
            // Decorating images for UI.
            this.visitorDecorator.decorateImageForUi(visitors);
        }

        return Lists.newArrayList(visitors);
    }
    
    /**
	 * This method fetches the employee details from employee table based on the
	 * employee id and retrieve the host details to send an intimation email for
	 * visitor
	 * 
	 * @param visitor : input visitor details
	 * @return : validation message
	 */
	private String sendEmailToHost(Visitor visitor) {
		Employee employee = visitor.getTimings().get(0).getEmployee();
		if (!Objects.isNull(employee)) {
			emailService.sendHostEmail(visitor.getFullName(), employee.getFirstName(), employee.getEmail());
			return Constants.HOST_EMAIL_SENT_MESSAGE;
		} else
			return Constants.HOST_NOT_FOUND_MESSAGE;
	}


}
