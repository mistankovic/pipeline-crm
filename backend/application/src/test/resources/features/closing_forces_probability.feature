Feature: Closing a deal settles its probability

  While a deal is open the salesperson's own judgement of the probability stands.
  The moment it closes, judgement stops mattering: a won deal is 100% and a lost deal is
  0%, whatever anybody typed before.

  Background:
    Given a salesperson "Sam"
    And a company "Acme"

  Scenario: Winning forces the probability to 100
    Given Sam owns a deal "Acme renewal" in stage NEGOTIATION worth 10000 EUR at 30% probability
    And a CALL has been logged against "Acme renewal"
    When Sam moves "Acme renewal" to CLOSED_WON
    Then the deal "Acme renewal" has probability 100

  Scenario: Losing forces the probability to 0
    Given Sam owns a deal "Acme renewal" in stage PROPOSAL worth 10000 EUR at 80% probability
    When Sam moves "Acme renewal" to CLOSED_LOST
    Then the deal "Acme renewal" has probability 0

  Scenario: Advancing inside the pipeline leaves the probability alone
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR at 25% probability
    When Sam moves "Acme renewal" to QUALIFIED
    Then the deal "Acme renewal" has probability 25

  Scenario: A closed deal's value can no longer be edited
    Given Sam owns a deal "Acme renewal" in stage PROPOSAL worth 10000 EUR at 80% probability
    And Sam moves "Acme renewal" to CLOSED_LOST
    When Sam tries to reprice "Acme renewal" to 20000 EUR
    Then the change is rejected because the deal is closed

  Scenario: A closed deal's probability can no longer be edited
    Given Sam owns a deal "Acme renewal" in stage PROPOSAL worth 10000 EUR at 80% probability
    And Sam moves "Acme renewal" to CLOSED_LOST
    When Sam tries to reweight "Acme renewal" to 90%
    Then the change is rejected because the deal is closed

  Scenario: An open deal can be repriced
    Given Sam owns a deal "Acme renewal" in stage PROPOSAL worth 10000 EUR at 80% probability
    When Sam reprices "Acme renewal" to 20000 EUR
    Then the deal "Acme renewal" is worth 20000 EUR
