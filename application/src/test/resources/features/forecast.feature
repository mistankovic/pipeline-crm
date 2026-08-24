Feature: Open-deal forecast
  Forecast is the sum of (value × probability / 100) for open deals.
  Closed deals are excluded. Results can be grouped by owner or by stage.
  Stage grouping includes every open stage even when its total is 0.
  Amounts in different currencies are not mixed.

  Background:
    Given a sales user "alice" exists
    And a sales user "bob" exists

  Scenario: owner grouping uses only open deals
    Given "alice" owns an open deal worth 1000 USD at probability 50
    And "alice" owns a CLOSED_WON deal worth 9000 USD at probability 100
    And "bob" owns an open deal worth 200 USD at probability 25
    When the forecast is grouped by owner in USD
    Then the forecast for "alice" is 500.00 USD
    And the forecast for "bob" is 50.00 USD

  Scenario: stage grouping includes empty open stages
    Given "alice" owns a LEAD deal worth 100 USD at probability 10
    And "alice" owns a CLOSED_LOST deal worth 500 USD at probability 0
    When the forecast is grouped by stage in USD
    Then the forecast for stage LEAD is 10.00 USD
    And the forecast for stage QUALIFIED is 0.00 USD
    And the forecast for stage PROPOSAL is 0.00 USD
    And the forecast for stage NEGOTIATION is 0.00 USD

  Scenario: an empty pipeline has no owner buckets
    When the forecast is grouped by owner in USD
    Then there are no owner forecast buckets

  Scenario: mixed currencies are rejected
    Given "alice" owns an open deal worth 1000 USD at probability 50
    And "bob" owns an open deal worth 200 EUR at probability 25
    When the forecast is grouped by owner in USD
    Then the forecast is rejected as mixed currency

  Scenario: requesting a currency none of the open deals use is rejected
    Given "alice" owns an open deal worth 1000 USD at probability 50
    When the forecast is grouped by owner in EUR
    Then the forecast is rejected as mixed currency
