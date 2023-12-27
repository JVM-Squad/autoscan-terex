package com.gunnarro.android.terex.service;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RecruitmentService {

    @Inject
    public RecruitmentService() {
    }

    public void addRecruitmentCompany() {

    }

    public String[] getRecruitmentNames() {
        return new String[]{"Norway Consulting AS", "Technogarden", "IT-Verket"};
    }

    public String[] getProjectNames() {
        return new String[]{"catalystOne solutions", "Nets efaktura"};
    }
}
