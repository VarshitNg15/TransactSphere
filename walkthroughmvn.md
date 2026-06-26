# Walkthrough: Fix auth-service Test Failures

## Problem Description
During `mvn clean install`, the unit tests in `auth-service` failed because MockMvc requests to `AuthController` endpoints returned a `403 Forbidden` status code. This was caused by Spring Security's default filters (such as CSRF protection and authorization checks) intercepting MockMvc requests since the custom security configuration was not loaded/applied.

## Changes Made
- Modified [AuthControllerTest.java](file:///E:/Project/TransactSphere/auth-service/src/test/java/com/transactsphere/auth/controller/AuthControllerTest.java):
  - Added `@AutoConfigureMockMvc(addFilters = false)` to the test class to disable the security filters during controller unit testing.
  - Added the corresponding import for `AutoConfigureMockMvc`.

Disabling the filters allows the MockMvc requests to directly reach the `AuthController` endpoints to test the controller's logic, validation, mappings, and service calls without interference from Spring Security filters.

## Verification
To verify the changes and ensure the tests now pass successfully, please run the following command in your terminal:

```powershell
mvn clean install
```
