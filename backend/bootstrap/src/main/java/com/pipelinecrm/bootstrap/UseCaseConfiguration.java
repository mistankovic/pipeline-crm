package com.pipelinecrm.bootstrap;

import com.pipelinecrm.application.port.in.ChangeDealStage;
import com.pipelinecrm.application.port.in.CorrectContact;
import com.pipelinecrm.application.port.in.CreateCompany;
import com.pipelinecrm.application.port.in.CreateContact;
import com.pipelinecrm.application.port.in.RenameCompany;
import com.pipelinecrm.application.port.in.CreateDeal;
import com.pipelinecrm.application.port.in.ListCompanies;
import com.pipelinecrm.application.port.in.ListContacts;
import com.pipelinecrm.application.port.in.ListUsers;
import com.pipelinecrm.application.port.in.LogActivity;
import com.pipelinecrm.application.port.in.ProduceForecast;
import com.pipelinecrm.application.port.in.RepriceDeal;
import com.pipelinecrm.application.port.in.ReweightDeal;
import com.pipelinecrm.application.port.in.SignIn;
import com.pipelinecrm.application.port.in.ViewContactTimeline;
import com.pipelinecrm.application.port.in.ViewDeal;
import com.pipelinecrm.application.port.in.ViewPipeline;
import com.pipelinecrm.application.port.out.AccessTokenIssuer;
import com.pipelinecrm.application.port.out.ActivityRepository;
import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.application.port.out.IdentifierFactory;
import com.pipelinecrm.application.port.out.PasswordChecker;
import com.pipelinecrm.application.port.out.Transactions;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.application.usecase.ActivitySubjects;
import com.pipelinecrm.application.usecase.ChangeDealStageInteractor;
import com.pipelinecrm.application.usecase.CorrectContactInteractor;
import com.pipelinecrm.application.usecase.CreateCompanyInteractor;
import com.pipelinecrm.application.usecase.CreateContactInteractor;
import com.pipelinecrm.application.usecase.RenameCompanyInteractor;
import com.pipelinecrm.application.usecase.CreateDealInteractor;
import com.pipelinecrm.application.usecase.DealHistory;
import com.pipelinecrm.application.usecase.DealMutations;
import com.pipelinecrm.application.usecase.ListCompaniesInteractor;
import com.pipelinecrm.application.usecase.ListContactsInteractor;
import com.pipelinecrm.application.usecase.ListUsersInteractor;
import com.pipelinecrm.application.usecase.LogActivityInteractor;
import com.pipelinecrm.application.usecase.Parties;
import com.pipelinecrm.application.usecase.ProduceForecastInteractor;
import com.pipelinecrm.application.usecase.RepriceDealInteractor;
import com.pipelinecrm.application.usecase.ReweightDealInteractor;
import com.pipelinecrm.application.usecase.SignInInteractor;
import com.pipelinecrm.application.usecase.Timelines;
import com.pipelinecrm.application.usecase.ViewContactTimelineInteractor;
import com.pipelinecrm.application.usecase.ViewDealInteractor;
import com.pipelinecrm.application.usecase.ViewPipelineInteractor;
import com.pipelinecrm.application.usecase.WritingPorts;
import com.pipelinecrm.domain.forecast.ForecastCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Where the use cases are assembled.
 *
 * <p>Not one class in `application` is annotated, so none of them can be found by scanning:
 * they are ordinary objects, and this is the file that news them up. That is the price of the
 * dependency rule and also the proof of it — the same graph is built by hand in the acceptance
 * tests, with no Spring at all, and the use cases cannot tell the difference.
 */
@Configuration
public class UseCaseConfiguration {

    @Bean
    public Parties parties(CompanyRepository companies, UserRepository users) {
        return new Parties(companies, users);
    }

    @Bean
    public DealHistory dealHistory(DealRepository deals, ActivityRepository activities) {
        return new DealHistory(deals, activities);
    }

    @Bean
    public Timelines timelines(Parties parties) {
        return new Timelines(parties);
    }

    @Bean
    public DealMutations dealMutations(DealHistory history, Parties parties, Transactions transactions) {
        return new DealMutations(history, parties, transactions);
    }

    @Bean
    public ActivitySubjects activitySubjects(DealRepository deals, ContactRepository contacts) {
        return new ActivitySubjects(deals, contacts);
    }

    @Bean
    public WritingPorts writingPorts(IdentifierFactory identifiers, Transactions transactions) {
        return new WritingPorts(identifiers, transactions);
    }

    @Bean
    public ForecastCalculator forecastCalculator() {
        return new ForecastCalculator();
    }

    @Bean
    public SignIn signIn(UserRepository users, PasswordChecker passwords, AccessTokenIssuer tokens) {
        return new SignInInteractor(users, passwords, tokens);
    }

    @Bean
    public ListUsers listUsers(UserRepository users) {
        return new ListUsersInteractor(users);
    }

    @Bean
    public ListCompanies listCompanies(CompanyRepository companies) {
        return new ListCompaniesInteractor(companies);
    }

    @Bean
    public CreateCompany createCompany(CompanyRepository companies, WritingPorts writing) {
        return new CreateCompanyInteractor(companies, writing);
    }

    @Bean
    public RenameCompany renameCompany(CompanyRepository companies, Parties parties, WritingPorts writing) {
        return new RenameCompanyInteractor(companies, parties, writing);
    }

    @Bean
    public ListContacts listContacts(ContactRepository contacts) {
        return new ListContactsInteractor(contacts);
    }

    @Bean
    public CreateContact createContact(ContactRepository contacts, Parties parties, WritingPorts writing) {
        return new CreateContactInteractor(contacts, parties, writing);
    }

    @Bean
    public CorrectContact correctContact(ContactRepository contacts, WritingPorts writing) {
        return new CorrectContactInteractor(contacts, writing);
    }

    @Bean
    public ViewContactTimeline viewContactTimeline(ContactRepository contacts, ActivityRepository activities,
                                                   Timelines timelines) {
        return new ViewContactTimelineInteractor(contacts, activities, timelines);
    }

    @Bean
    public ViewPipeline viewPipeline(DealRepository deals, Parties parties) {
        return new ViewPipelineInteractor(deals, parties);
    }

    @Bean
    public ViewDeal viewDeal(DealHistory history, Parties parties, Timelines timelines) {
        return new ViewDealInteractor(history, parties, timelines);
    }

    @Bean
    public CreateDeal createDeal(DealRepository deals, Parties parties, WritingPorts writing) {
        return new CreateDealInteractor(deals, parties, writing);
    }

    @Bean
    public ChangeDealStage changeDealStage(DealMutations mutations, DealHistory history) {
        return new ChangeDealStageInteractor(mutations, history);
    }

    @Bean
    public RepriceDeal repriceDeal(DealMutations mutations) {
        return new RepriceDealInteractor(mutations);
    }

    @Bean
    public ReweightDeal reweightDeal(DealMutations mutations) {
        return new ReweightDealInteractor(mutations);
    }

    @Bean
    public LogActivity logActivity(ActivityRepository activities, ActivitySubjects subjects,
                                   Parties parties, LogActivityInteractor.Recording recording) {
        return new LogActivityInteractor(activities, subjects, parties, recording);
    }

    @Bean
    public LogActivityInteractor.Recording recording(WritingPorts writing, Clock clock) {
        return new LogActivityInteractor.Recording(writing, clock);
    }

    @Bean
    public ProduceForecast produceForecast(DealRepository deals, Parties parties, ForecastCalculator calculator) {
        return new ProduceForecastInteractor(deals, parties, calculator);
    }
}
