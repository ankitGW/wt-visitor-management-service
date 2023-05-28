package com.wissen.service.impl;

import com.wissen.decorator.VisitorDecorator;
import com.wissen.dto.FilterRequest;
import com.wissen.enrich.FilterSpecification;
import com.wissen.entity.Visitor;
import com.wissen.exceptions.VisitorManagementException;
import com.wissen.repository.VisitorRepository;
import com.wissen.service.VisitorService;
import com.wissen.util.VisitorManagementUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

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
     * @param visitor
     * @return savedVisitor
     */
    @Override
    @Transactional
    public Visitor saveVisitorDetails(Visitor visitor) {

        //Mandatory for Visitor Image
        if (StringUtils.isBlank(visitor.getVisitorImageBase64())) {
            throw new VisitorManagementException("Visitor image can not be empty.");
        }

        // decorating before saving
        visitorDecorator.decorateBeforeSaving(visitor);

        //save/update the details
        Visitor savedVisitor = this.visitorRepository.save(visitor);

        // decorating after saving
        visitorDecorator.decorateAfterSaving(savedVisitor);

        return savedVisitor;
    }

    /**
     * Method to fetch values from the Visitor table
     * Dynamic Query will be formed based on the request filter
     * If the list is empty then all visitor will be fetched
     * Else will return only specific results     *
     *
     * @param requestFilters
     * @return List of visitor response based on the filter request
     */
    @Override
    public List<Visitor> fetchVisitorsDetails(List<FilterRequest> requestFilters) {
        List<Visitor> visitors = new ArrayList<>();
        if (CollectionUtils.isEmpty(requestFilters)) {
            visitors.addAll(visitorRepository.findAll());
        } else {
            Specification<Visitor> specificationRequest = filterSpecification.getSpecificationFromFilters(requestFilters);
            visitors.addAll(visitorRepository.findAll(specificationRequest));
        }

        // Decorating images for UI.
        visitors.forEach(visitor -> {
            visitor.setVisitorImageBase64(VisitorManagementUtils.convertByteToBase64(visitor.getVisitorImage()));
            visitor.setVisitorImage(null);
        });

        return visitors;
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

}
