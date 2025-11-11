package com.cs102.service;

import com.cs102.model.SecurityEvent;

/**
 * Interface for receiving real-time security alerts
 */
public interface SecurityAlertListener {
    /**
     * Called when a new security event is logged
     * @param event The security event that was logged
     */
    void onSecurityAlert(SecurityEvent event);
}
