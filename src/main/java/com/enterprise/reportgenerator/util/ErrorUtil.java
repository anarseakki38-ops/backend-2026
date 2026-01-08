package com.enterprise.reportgenerator.util;

public class ErrorUtil {

    public static String sanitizeErrorMessage(String message) {
        if (message == null)
            return "Unknown error during execution";

        String lowerMessage = message.toLowerCase();

        // Sanitize common technical errors
        if (lowerMessage.contains("connectexception") ||
                lowerMessage.contains("mailconnectexception") ||
                lowerMessage.contains("connection refused") ||
                lowerMessage.contains("unreachable")) {
            return "Connection failed (Mail server or Database unreachable)";
        }

        if (lowerMessage.contains("ora-") || lowerMessage.contains("bad sql grammar")
                || lowerMessage.contains("statementcallback")) {
            return "Database error occurred during query execution. Please check your SQL syntax or table names.";
        }

        if (lowerMessage.contains("filenotfoundexception") || lowerMessage.contains("accessdeniedexception")) {
            return "File system error: Could not save or access report file";
        }

        if (lowerMessage.contains("timeout")) {
            return "Operation timed out. Please try again later.";
        }

        return "Operation failed due to an internal error";
    }
}
