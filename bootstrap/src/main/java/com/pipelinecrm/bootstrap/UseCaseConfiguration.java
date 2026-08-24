package com.pipelinecrm.bootstrap;

import com.pipelinecrm.application.port.out.ActivityRepository;
import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.application.port.out.PasswordHasher;
import com.pipelinecrm.application.port.out.TokenIssuer;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.application.usecase.ChangeDealStageService;
import com.pipelinecrm.application.usecase.CompanyServices;
import com.pipelinecrm.application.usecase.ContactServices;
import com.pipelinecrm.application.usecase.CreateDealService;
import com.pipelinecrm.application.usecase.ForecastService;
import com.pipelinecrm.application.usecase.ListDealsService;
import com.pipelinecrm.application.usecase.ListUsersService;
import com.pipelinecrm.application.usecase.LoginService;
import com.pipelinecrm.application.usecase.RecordActivityService;
import com.pipelinecrm.application.usecase.UpdateDealService;
import com.pipelinecrm.application.usecase.ViewDealService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    LoginService loginService(UserRepository users, PasswordHasher hasher, TokenIssuer tokens) {
        return new LoginService(users, hasher, tokens);
    }

    @Bean
    ListUsersService listUsersService(UserRepository users) {
        return new ListUsersService(users);
    }

    @Bean
    CompanyServices companyServices(CompanyRepository companies) {
        return new CompanyServices(companies);
    }

    @Bean
    ContactServices contactServices(ContactRepository contacts, CompanyRepository companies) {
        return new ContactServices(contacts, companies);
    }

    @Bean
    CreateDealService createDealService(DealRepository deals, CompanyRepository companies, UserRepository users) {
        return new CreateDealService(deals, companies, users);
    }

    @Bean
    UpdateDealService updateDealService(DealRepository deals) {
        return new UpdateDealService(deals);
    }

    @Bean
    ListDealsService listDealsService(DealRepository deals) {
        return new ListDealsService(deals);
    }

    @Bean
    ViewDealService viewDealService(DealRepository deals) {
        return new ViewDealService(deals);
    }

    @Bean
    ChangeDealStageService changeDealStageService(DealRepository deals, UserRepository users) {
        return new ChangeDealStageService(deals, users);
    }

    @Bean
    RecordActivityService recordActivityService(ActivityRepository activities, DealRepository deals, Clock clock) {
        return new RecordActivityService(activities, deals, clock);
    }

    @Bean
    ForecastService forecastService(DealRepository deals) {
        return new ForecastService(deals);
    }
}
