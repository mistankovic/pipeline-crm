package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.CompanyView;

import java.util.UUID;

/** Correct a company's name, keeping everything that points at it. */
public interface RenameCompany {

    CompanyView handle(UUID companyId, String newName);
}
