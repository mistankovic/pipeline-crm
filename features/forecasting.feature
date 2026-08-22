Feature: Pipeline forecasting
  Forecast is the sum of (value × probability) for open deals only.
  Totals are grouped by owner or by stage and never mix currencies.

  Background:
    Given a sales user "alex" exists
    And a sales user "blair" exists
    And a company "Northwind Robotics" exists
    And "alex" owns a deal "Arm retrofit" at company "Northwind Robotics"
    And deal "Arm retrofit" is worth 100000 USD at 50% probability
    And deal "Arm retrofit" is in stage PROPOSAL
    And "blair" owns a deal "Vision pack" at company "Northwind Robotics"
    And deal "Vision pack" is worth 40000 USD at 25% probability
    And deal "Vision pack" is in stage LEAD
    And "alex" owns a deal "EU service" at company "Northwind Robotics"
    And deal "EU service" is worth 20000 EUR at 10% probability
    And deal "EU service" is in stage QUALIFIED

  Scenario: Forecast by owner keeps currencies separate
    When the forecast is grouped by owner
    Then owner "alex" has weighted 50000 USD
    And owner "alex" has weighted 2000 EUR
    And owner "blair" has weighted 10000 USD

  Scenario: Forecast by stage
    When the forecast is grouped by stage
    Then stage "PROPOSAL" has weighted 50000 USD
    And stage "LEAD" has weighted 10000 USD
    And stage "QUALIFIED" has weighted 2000 EUR

  Scenario: Closed deals are excluded from the forecast
    Given deal "Arm retrofit" has a meeting activity "Factory tour"
    And "alex" moves deal "Arm retrofit" to CLOSED_WON
    When the forecast is grouped by owner
    Then owner "alex" has weighted 2000 EUR
    And owner "alex" does not have a USD total
    And the grand total includes 10000 USD
    And the grand total includes 2000 EUR

  Scenario: A 0% open deal contributes nothing
    Given deal "Vision pack" is worth 40000 USD at 0% probability
    When the forecast is grouped by owner
    Then owner "blair" has weighted 0 USD
