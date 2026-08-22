Feature: Forecasting the open pipeline

  The forecast is the sum of value times probability over the deals that are still live.
  Won and lost deals are history and never appear in it. Currencies are never converted,
  so a pipeline in two currencies produces two lines.

  Background:
    Given a salesperson "Sam"
    And a salesperson "Robin"
    And a company "Acme"

  Scenario: An empty pipeline forecasts nothing
    When the forecast is produced by OWNER
    Then the forecast is empty

  Scenario: A single deal is weighted by its probability
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR at 40% probability
    When the forecast is produced by OWNER
    Then the forecast for Sam in EUR is 4000

  Scenario: Deals of one owner are added together
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR at 50% probability
    And Sam owns a deal "Acme expansion" in stage PROPOSAL worth 4000 EUR at 25% probability
    When the forecast is produced by OWNER
    Then the forecast for Sam in EUR is 6000

  Scenario: Owners are reported separately
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR at 50% probability
    And Robin owns a deal "Acme support" in stage LEAD worth 10000 EUR at 10% probability
    When the forecast is produced by OWNER
    Then the forecast for Sam in EUR is 5000
    And the forecast for Robin in EUR is 1000

  Scenario: The forecast can be sliced by stage instead
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR at 50% probability
    And Sam owns a deal "Acme expansion" in stage PROPOSAL worth 10000 EUR at 20% probability
    When the forecast is produced by STAGE
    Then the forecast for stage LEAD in EUR is 5000
    And the forecast for stage PROPOSAL in EUR is 2000

  Scenario: A won deal drops out of the forecast
    Given Sam owns a deal "Acme renewal" in stage NEGOTIATION worth 10000 EUR at 90% probability
    And a MEETING has been logged against "Acme renewal"
    And Sam moves "Acme renewal" to CLOSED_WON
    When the forecast is produced by OWNER
    Then the forecast is empty

  Scenario: A lost deal drops out of the forecast
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR at 50% probability
    And Sam moves "Acme renewal" to CLOSED_LOST
    When the forecast is produced by OWNER
    Then the forecast is empty

  Scenario: Two currencies produce two lines and are never added together
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR at 50% probability
    And Sam owns a deal "Acme US" in stage LEAD worth 10000 USD at 50% probability
    When the forecast is produced by OWNER
    Then the forecast for Sam in EUR is 5000
    And the forecast for Sam in USD is 5000
    And the forecast has 2 lines
