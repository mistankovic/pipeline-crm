Feature: Signing in

  The demo authenticates with an email address and a password and hands back a token the
  browser uses on later calls. A wrong password and an unknown user are answered
  identically, so that the failure does not reveal who has an account.

  Users are not created through this system: they are provisioned by a database migration
  (see docs/domain-decisions.md, decision D-13). "Given a salesperson with password" is
  therefore a fixture describing seeded data, not a use case.

  Background:
    Given a salesperson "Sam" with password "correct-horse"

  Scenario: A known user with the right password is signed in
    When "sam@example.com" signs in with password "correct-horse"
    Then the sign-in succeeds and returns a token for Sam

  Scenario: A known user with the wrong password is refused
    When "sam@example.com" signs in with password "wrong"
    Then the sign-in is refused with the same message as an unknown user

  Scenario: An unknown user is refused
    When "nobody@example.com" signs in with password "correct-horse"
    Then the sign-in is refused with the same message as an unknown user
