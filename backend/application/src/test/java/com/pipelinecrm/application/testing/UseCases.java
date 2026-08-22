package com.pipelinecrm.application.testing;

import com.pipelinecrm.application.port.in.ChangeDealStage;
import com.pipelinecrm.application.port.in.CreateCompany;
import com.pipelinecrm.application.port.in.CreateContact;
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
import com.pipelinecrm.application.usecase.ActivitySubjects;
import com.pipelinecrm.application.usecase.ChangeDealStageInteractor;
import com.pipelinecrm.application.usecase.CreateCompanyInteractor;
import com.pipelinecrm.application.usecase.CreateContactInteractor;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * The whole application, wired over in-memory doubles.
 *
 * <p>This is the same object graph the Spring configuration will build in Stage 5, assembled
 * by hand. That the graph can be built by hand at all is the point of the architecture: the
 * use cases do not know that a framework exists.
 */
public final class UseCases {

    public static final Instant NOW = Instant.parse("2026-03-01T09:00:00Z");

    public final InMemoryUsers users = new InMemoryUsers();
    public final InMemoryCompanies companies = new InMemoryCompanies();
    public final InMemoryContacts contacts = new InMemoryContacts();
    public final InMemoryDeals deals = new InMemoryDeals();
    public final InMemoryActivities activities = new InMemoryActivities();
    public final StubPasswords passwords = new StubPasswords();
    public final StubTokens tokens = new StubTokens();
    public final ImmediateTransactions transactions = new ImmediateTransactions();
    public final SequentialIdentifiers identifiers = new SequentialIdentifiers();

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Parties parties = new Parties(companies, users);
    private final DealHistory history = new DealHistory(deals, activities);
    private final WritingPorts writing = new WritingPorts(identifiers, transactions);
    private final Timelines timelines = new Timelines(parties);
    private final DealMutations mutations = new DealMutations(history, parties, transactions);

    public final SignIn signIn = new SignInInteractor(users, passwords, tokens);
    public final CreateCompany createCompany = new CreateCompanyInteractor(companies, writing);
    public final ListCompanies listCompanies = new ListCompaniesInteractor(companies);
    public final CreateContact createContact = new CreateContactInteractor(contacts, parties, writing);
    public final ListContacts listContacts = new ListContactsInteractor(contacts);
    public final ListUsers listUsers = new ListUsersInteractor(users);
    public final CreateDeal createDeal = new CreateDealInteractor(deals, parties, writing);
    public final ViewPipeline viewPipeline = new ViewPipelineInteractor(deals, parties);
    public final ViewDeal viewDeal = new ViewDealInteractor(history, parties, timelines);
    public final ChangeDealStage changeDealStage = new ChangeDealStageInteractor(mutations, history);
    public final RepriceDeal repriceDeal = new RepriceDealInteractor(mutations);
    public final ReweightDeal reweightDeal = new ReweightDealInteractor(mutations);
    public final LogActivity logActivity = new LogActivityInteractor(activities,
            new ActivitySubjects(deals, contacts), parties,
            new LogActivityInteractor.Recording(writing, clock));
    public final ViewContactTimeline viewContactTimeline =
            new ViewContactTimelineInteractor(contacts, activities, timelines);
    public final ProduceForecast produceForecast =
            new ProduceForecastInteractor(deals, parties, new ForecastCalculator());
}
