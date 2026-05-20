# Clean Code Naming Refactoring Templates

Use these exact structural patterns when transforming poorly named legacy code.

### Example 1: Intent-Revealing Refactoring
*   **Legacy:**
    ```java
    // checks if user can login
    boolean chk(User u) {
        if (u.st == 1 && u.getDays() < 30) return true;
        return false;
    }
    ```
*   **Clean Code Standard:**
    ```java
    boolean isUserEligibleForLogin(User user) {
        boolean isActive = (user.getStatus() == ACCOUNT_STATUS_ACTIVE);
        boolean isPasswordExpired = (user.getDaysSinceLastPasswordChange() >= PASSWORD_EXPIRATION_LIMIT_DAYS);
        
        return isActive && !isPasswordExpired;
    }