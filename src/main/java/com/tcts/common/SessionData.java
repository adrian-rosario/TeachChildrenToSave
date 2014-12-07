package com.tcts.common;

import com.tcts.model2.BankAdmin;
import com.tcts.model2.SiteAdmin;
import com.tcts.model2.Teacher;
import com.tcts.model2.Volunteer;
import com.tcts.model2.User;

import javax.servlet.http.HttpSession;

/**
 * An instance of this class will be stored in the session. It contains all
 * of the data that will potentially be passed around while navigating the
 * the site.
 */
public class SessionData {
    private final static String sessionKey = "sessionData";

    /**
     * If there is a logged-in user session, this retrieves it from the
     * HttpSession object. If not, then it raises a "NotLoggedInException".
     * Use this at the top of any controller method that expects the user
     * to be already logged in.
     */
    public static SessionData fromSession(HttpSession session) {
        Object sessionData = session.getAttribute(sessionKey);
        if (sessionData == null) {
            throw new NotLoggedInException();
        } else {
            return (SessionData) sessionData;
        }
    }

    /**
     * This ensures that there is NOT an active session by throwing a
     * ActiveSessionException if there is one and doing nothing if there isn't.
     * It is placed in this class because SessionData.fromSession() is the
     * standard way to ensure that there IS a session.
     */
    public static void ensureNoActiveSession(HttpSession session) {
        Object sessionData = session.getAttribute(sessionKey);
        if (sessionData != null) {
            throw new ActiveSessionException();
        }
    }

    /**
     * This is used to put a SessionData into the session which should be
     * done after a user has successfully logged in.
     */
    public static SessionData beginNewSession(HttpSession session) {
        SessionData sessionData = new SessionData();
        session.setAttribute(sessionKey, sessionData);
        return sessionData;
    }


	private boolean isAuthenticated;
    private User user; // Will be null if no one is logged in
    private Teacher teacher; // Will be null unless a teacher is logged in.
    private Volunteer volunteer; // Will be null unless a volunteer is logged in.
    private BankAdmin bankAdmin; // Will be null unless a bank admin is logged in.
    private SiteAdmin siteAdmin; // Will be null unless a site admin is logged in.

    /**
     * Constructor is private; use static methods beginNewSession() and/or
     * fromSession() to obtain an instance.
     */
    private SessionData() {
    }

	public boolean isAuthenticated() {
		return isAuthenticated;
	}
	public void setAuthenticated(boolean isAuthenticated) {
		this.isAuthenticated = isAuthenticated;
	}

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public Volunteer getVolunteer() {
        return volunteer;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
    }

    public BankAdmin getBankAdmin() {
        return bankAdmin;
    }

    public void setBankAdmin(BankAdmin bankAdmin) {
        this.bankAdmin = bankAdmin;
    }

    public SiteAdmin getSiteAdmin() {
        return siteAdmin;
    }

    public void setSiteAdmin(SiteAdmin siteAdmin) {
        this.siteAdmin = siteAdmin;
    }
}
