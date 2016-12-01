package com.tcts.database;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.tcts.common.PrettyPrintingDate;
import com.tcts.datamodel.ApprovalStatus;
import com.tcts.datamodel.Bank;
import com.tcts.datamodel.BankAdmin;
import com.tcts.datamodel.Event;
import com.tcts.datamodel.School;
import com.tcts.datamodel.SiteAdmin;
import com.tcts.datamodel.SiteStatistics;
import com.tcts.datamodel.Teacher;
import com.tcts.datamodel.User;
import com.tcts.datamodel.UserType;
import com.tcts.datamodel.Volunteer;
import com.tcts.exception.AllowedDateAlreadyInUseException;
import com.tcts.exception.AllowedTimeAlreadyInUseException;
import com.tcts.exception.EmailAlreadyInUseException;
import com.tcts.exception.InconsistentDatabaseException;
import com.tcts.exception.NoSuchAllowedDateException;
import com.tcts.exception.NoSuchAllowedTimeException;
import com.tcts.exception.NoSuchBankException;
import com.tcts.exception.NoSuchEventException;
import com.tcts.exception.NoSuchSchoolException;
import com.tcts.exception.NoSuchUserException;
import com.tcts.exception.TeacherHasEventsException;
import com.tcts.exception.VolunteerHasEventsException;
import com.tcts.formdata.AddAllowedDateFormData;
import com.tcts.formdata.AddAllowedTimeFormData;
import com.tcts.formdata.CreateBankFormData;
import com.tcts.formdata.CreateEventFormData;
import com.tcts.formdata.CreateSchoolFormData;
import com.tcts.formdata.EditBankFormData;
import com.tcts.formdata.EditPersonalDataFormData;
import com.tcts.formdata.EditSchoolFormData;
import com.tcts.formdata.EditVolunteerPersonalDataFormData;
import com.tcts.formdata.EventRegistrationFormData;
import com.tcts.formdata.SetBankSpecificFieldLabelFormData;
import com.tcts.formdata.TeacherRegistrationFormData;
import com.tcts.formdata.VolunteerRegistrationFormData;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.tcts.database.DatabaseField.*;


/**
 * A facade that implements the database functionality using DynamoDB.
 */
public class DynamoDBDatabase implements DatabaseFacade {

    // ========== Instance Variables and Constructor ==========

    private final DatabaseFacade delegate; // FIXME: Only temporary. Delegate some calls here until fully implemented
    private final Tables tables;


    public DynamoDBDatabase(DatabaseFacade delegate) {
        this.delegate = delegate;
        this.tables = getTables(connectToDB());
    }

    // ========== Static Methods Shared by DynamoDBSetup ==========

    static DynamoDB connectToDB() {
        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient();
        dynamoDBClient.withEndpoint("http://localhost:8000"); // FIXME: Make this a config property.
        return new DynamoDB(dynamoDBClient);
    }

    static class Tables {
        final Table siteSettingsTable;
        final Table allowedDatesTable;
        final Table allowedTimesTable;
        final Table eventTable;
        final Table bankTable;
        final Table userTable;
        final Table schoolTable;

        /**
         * Constructor.
         */
        public Tables(
                final Table siteSettingsTable,
                final Table allowedDatesTable,
                final Table allowedTimesTable,
                final Table eventTable,
                final Table bankTable,
                final Table userTable,
                final Table schoolTable
        ) {
            this.siteSettingsTable = siteSettingsTable;
            this.allowedDatesTable = allowedDatesTable;
            this.allowedTimesTable = allowedTimesTable;
            this.eventTable = eventTable;
            this.bankTable = bankTable;
            this.userTable = userTable;
            this.schoolTable = schoolTable;
        }
    }

    static DynamoDBDatabase.Tables getTables(DynamoDB dynamoDB) {
        dynamoDB.getTable("SiteSettings");
        Table siteSettingsTable = dynamoDB.getTable("SiteSettings");
        Table allowedDatesTable = dynamoDB.getTable("AllowedDates");
        Table allowedTimesTable = dynamoDB.getTable("AllowedTimes");
        Table eventTable = dynamoDB.getTable("Event");
        Table bankTable = dynamoDB.getTable("Bank");
        Table userTable = dynamoDB.getTable("User");
        Table schoolTable = dynamoDB.getTable("School");
        return new DynamoDBDatabase.Tables(siteSettingsTable, allowedDatesTable, allowedTimesTable, eventTable, bankTable, userTable, schoolTable);
    }

    // ========== Special Plumbing ==========

    /**
     * When this is called, it will create a single, unique ID.
     * <p>
     * We happen to be using the following approach: pick a random
     * positive long. Count on luck for it to never collide. It's
     * not the most perfect algorithm in the world, but using the
     * birthday problem formula, we would need to issue about 430
     * million IDs to have a 1% chance of encountering a collision.
     */
    private String createUniqueId() {
        long randomNonNegativeLong = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        return Long.toString(randomNonNegativeLong);
    }

    /** Static class used when getting allowed times in the proper sort order. */
    private static class TimeAndSortKey implements Comparable<TimeAndSortKey> {
        final String timeStr;
        final int sortKey;

        /** Constructor. */
        TimeAndSortKey(String timeStr, int sortKey) {
            this.timeStr = timeStr;
            this.sortKey = sortKey;
        }

        @Override
        public int compareTo(TimeAndSortKey o) {
            return Integer.compare(this.sortKey, o.sortKey);
        }

        @Override
        public String toString() {
            return "TimeAndSortKey{" +
                    "timeStr='" + timeStr + '\'' +
                    ", sortKey=" + sortKey +
                    '}';
        }
    }

    /**
     * Helper that creates an AttributeUpdate for setting a particular field to a
     * particular string value. If the string is "" or null, then the attribute
     * will be deleted; if it has any other value then it will be set to that.
     *
     * @param field the DatabaseField to update
     * @param attributeValue the value to set it to, including "" or null.
     */
    private AttributeUpdate attributeUpdate(DatabaseField field, String attributeValue) {
        if (attributeValue == null || attributeValue.isEmpty()) {
            return new AttributeUpdate(field.name()).delete();
        } else {
            return new AttributeUpdate(field.name()).put(attributeValue);
        }
    }

    /**
     * Helper that creates an AttributeUpdate for setting a particular field to a
     * particular string value. If the string is "" or null, then the attribute
     * will be deleted; if it has any other value then it will be set to that.
     *
     * @param field the DatabaseField to update
     * @param attributeValue the value to set it to, including "" or null.
     */
    private AttributeUpdate intAttributeUpdate(DatabaseField field, int attributeValue) {
        return new AttributeUpdate(field.name()).put(attributeValue);
    }

    // ========== Methods for populating objects ==========


    /**
     * Creates a School object from the corresponding Item retrieved from DynamoDB. If passed
     * null, it returns null.
     */
    private School createSchoolFromDynamoDBItem(Item item) {
        if (item == null) {
            return null;
        }
        School school = new School();
        school.setSchoolId(item.getString(school_id.name()));
        school.setName(item.getString(school_name.name()));
        school.setAddressLine1(item.getString(school_addr1.name()));
        school.setCity(item.getString(school_city.name()));
        school.setState(item.getString(school_state.name()));
        school.setZip(item.getString(school_zip.name()));
        school.setCounty(item.getString(school_county.name()));
        school.setSchoolDistrict(item.getString(school_district.name()));
        school.setPhone(item.getString(school_phone.name()));
        school.setLmiEligible(item.getInt(school_lmi_eligible.name()));
        school.setSLC(item.getString(school_slc.name()));
        return school;
    }


    /**
     * Creates a Bank object from the corresponding Item retrieved from DynamoDB. If passed
     * null, it returns null.
     */
    private Bank createBankFromDynamoDBItem(Item item) {
        if (item == null) {
            return null;
        }
        Bank bank = new Bank();
        bank.setBankId(item.getString(bank_id.name()));
        bank.setBankName(item.getString(bank_name.name()));
        if (item.get(min_lmi_for_cra.name()) == null) {
            bank.setMinLMIForCRA(null); // An int field that nevertheless can store null
        } else {
            bank.setMinLMIForCRA(item.getInt(min_lmi_for_cra.name()));
        }
        if (item.getString(bank_specific_data_label.name()) == null) {
            bank.setBankSpecificDataLabel(""); // Use "" when there is a null in the DB
        } else {
            bank.setBankSpecificDataLabel(item.getString(bank_specific_data_label.name()));
        }
        return bank;
    }

    /**
     * Creates a User object from the corresponding Item retrieved from DynamoDB. It will
     * be of the appropriate concrete sub-type of User. If passed null, it returns null.
     * None of the linked data is filled in.
     */
    private User createUserFromDynamoDBItem(Item item) {
        if (item == null) {
            return null;
        }
        UserType userType = UserType.fromDBValue(item.getString(user_type.name()));
        User user;
        switch(userType) {
            case TEACHER: {
                Teacher teacher = new Teacher();
                teacher.setSchoolId(item.getString(user_organization_id.name()));
                user = teacher;
            } break;
            case VOLUNTEER: {
                Volunteer volunteer = new Volunteer();
                volunteer.setBankId(item.getString(user_organization_id.name()));
                volunteer.setApprovalStatus(ApprovalStatus.fromDBValue(item.getInt(user_approval_status.name())));
                volunteer.setBankSpecificData(item.getString(user_bank_specific_data.name()));
                user = volunteer;
            } break;
            case BANK_ADMIN: {
                BankAdmin bankAdmin = new BankAdmin();
                bankAdmin.setBankId(item.getString(user_organization_id.name()));
                bankAdmin.setApprovalStatus(ApprovalStatus.fromDBValue(item.getInt(user_approval_status.name())));
                bankAdmin.setBankSpecificData(item.getString(user_bank_specific_data.name()));
                user = bankAdmin;
            } break;
            case SITE_ADMIN: {
                SiteAdmin siteAdmin = new SiteAdmin();
                user = siteAdmin;
            } break;
            default: {
                throw new RuntimeException("Invalid type in case statement.");
            }
        }
        user.setUserId(item.getString(user_id.name()));
        user.setEmail(item.getString(user_email.name()));
        user.setHashedPassword(item.getString(user_hashed_password.name()));
        user.setSalt(item.getString(user_password_salt.name()));
        user.setFirstName(item.getString(user_first_name.name()));
        user.setLastName(item.getString(user_last_name.name()));
        user.setPhoneNumber(item.getString(user_phone_number.name()));
        user.setResetPasswordToken(item.getString(user_reset_password_token.name()));
        user.setUserType(userType);
        return user;
    }

    // ========== Methods of DatabaseFacade Class ==========

    @Override
    public int getFieldLength(DatabaseField field) {
        return delegate.getFieldLength(field);
    }

    @Override
    public User getUserById(String userId) throws SQLException, InconsistentDatabaseException {
        Item item = tables.userTable.getItem(new PrimaryKey(user_id.name(), userId));
        return createUserFromDynamoDBItem(item);
    }

    @Override
    public User getUserByEmail(String email) throws SQLException, InconsistentDatabaseException {
        Index userByEmail = tables.userTable.getIndex("byEmail");
        ItemCollection<QueryOutcome> users = userByEmail.query(new KeyAttribute(user_email.name(), email));
        User user = null;
        int numItems = 0;
        for (Item item : users) {
            user = createUserFromDynamoDBItem(item);
            numItems += 1;
        }
        if (numItems == 0) {
            return null;
        } else if (numItems == 1) {
            return user;
        } else {
            throw new InconsistentDatabaseException("More than one user with email address '" + email + "'.");
        }
    }

    @Override
    public void modifyUserPersonalFields(EditPersonalDataFormData formData) throws SQLException, EmailAlreadyInUseException, InconsistentDatabaseException {
        // This approach will CREATE the user if it doesn't exist. I THINK that behavior is fine.
        tables.userTable.updateItem(
                new PrimaryKey(user_id.name(), formData.getUserId()),
                attributeUpdate(user_email, formData.getEmail()),
                attributeUpdate(user_first_name, formData.getFirstName()),
                attributeUpdate(user_last_name, formData.getLastName()),
                attributeUpdate(user_phone_number, formData.getPhoneNumber()));
    }

    @Override
    public Volunteer modifyVolunteerPersonalFields(EditVolunteerPersonalDataFormData formData) throws SQLException, EmailAlreadyInUseException, InconsistentDatabaseException {
        return delegate.modifyVolunteerPersonalFields(formData); // FIXME: User Related
    }

    @Override
    public void modifyTeacherSchool(String userId, String organizationId) throws SQLException, NoSuchSchoolException, NoSuchUserException {
        // This approach will CREATE the user if it doesn't exist. I THINK that behavior is fine.
        // It also does not verify that the organization ID actually exists in the database.
        tables.userTable.updateItem(
                new PrimaryKey(user_id.name(), userId),
                attributeUpdate(user_organization_id, organizationId));
    }

    @Override
    public Teacher insertNewTeacher(TeacherRegistrationFormData formData, String hashedPassword, String salt) throws SQLException, NoSuchSchoolException, EmailAlreadyInUseException, NoSuchAlgorithmException, UnsupportedEncodingException {
        // NOTE: I'm choosing NOT to verify that the school ID is actually present in the database
        // FIXME: I *must* verify that the email is unique, and I don't do that yet.
        String newTeacherId = createUniqueId();
        tables.userTable.putItem(new Item()
                .withPrimaryKey(new PrimaryKey(user_id.name(), newTeacherId))
                .withString(user_type.name(), UserType.TEACHER.getDBValue())
                .withString(user_email.name(), formData.getEmail())
                .withString(user_first_name.name(), formData.getFirstName())
                .withString(user_last_name.name(), formData.getLastName())
                .withString(user_phone_number.name(), formData.getPhoneNumber())
                .withString(user_organization_id.name(), formData.getSchoolId())
                .withString(user_hashed_password.name(), hashedPassword)
                .withString(user_password_salt.name(), salt));
        Teacher result = new Teacher();
        result.setUserId(newTeacherId);
        result.setUserType(UserType.TEACHER);
        result.setEmail(formData.getEmail());
        result.setFirstName(formData.getFirstName());
        result.setLastName(formData.getLastName());
        result.setPhoneNumber(formData.getPhoneNumber());
        result.setSchoolId(formData.getSchoolId());
        result.setHashedPassword(hashedPassword);
        result.setSalt(salt);
        return result;
    }

    @Override
    public List<Event> getEventsByTeacher(String teacherId) throws SQLException {
        return delegate.getEventsByTeacher(teacherId);
    }

    @Override
    public List<Event> getAllAvailableEvents() throws SQLException {
        return delegate.getAllAvailableEvents();
    }

    @Override
    public List<Event> getEventsByVolunteer(String volunteerId) throws SQLException {
        return delegate.getEventsByVolunteer(volunteerId);
    }

    @Override
    public List<Event> getEventsByVolunteerWithTeacherAndSchool(String volunteerId) throws SQLException {
        return delegate.getEventsByVolunteerWithTeacherAndSchool(volunteerId);
    }

    @Override
    public void insertEvent(String teacherId, CreateEventFormData formData) throws SQLException {
        delegate.insertEvent(teacherId, formData);
    }

    @Override
    public void volunteerForEvent(String eventId, String volunteerId) throws SQLException, NoSuchEventException {
        delegate.volunteerForEvent(eventId, volunteerId);
    }

    @Override
    public List<Volunteer> getVolunteersByBank(String bankId) throws SQLException {
        return delegate.getVolunteersByBank(bankId);
    }

    @Override
    public BankAdmin getBankAdminByBank(String bankId) throws SQLException {
        return delegate.getBankAdminByBank(bankId);
    }

    @Override
    public Volunteer insertNewVolunteer(VolunteerRegistrationFormData formData, String hashedPassword, String salt) throws SQLException, NoSuchBankException, EmailAlreadyInUseException {
        return delegate.insertNewVolunteer(formData, hashedPassword, salt); // FIXME: User Related
    }

    @Override
    public Bank getBankById(String bankId) throws SQLException {
        Item item = tables.bankTable.getItem(new PrimaryKey(bank_id.name(), bankId));
        return createBankFromDynamoDBItem(item);
    }

    @Override
    public School getSchoolById(String schoolId) throws SQLException {
        Item item = tables.schoolTable.getItem(new PrimaryKey(school_id.name(), schoolId));
        return createSchoolFromDynamoDBItem(item);
    }

    @Override
    public List<School> getAllSchools() throws SQLException {
        List<School> result = new ArrayList<School>();
        // -- Get the schools --
        for (Item item : tables.schoolTable.scan()) {
            result.add(createSchoolFromDynamoDBItem(item));
        }
        // -- Sort by name --
        Collections.sort(result, new Comparator<School>() {
            @Override
            public int compare(School school1, School school2) {
                return school1.getName().compareTo(school2.getName());
            }
        });
        // -- Return the result --
        return result;
    }

    @Override
    public List<Bank> getAllBanks() throws SQLException {
        List<Bank> result = new ArrayList<Bank>();
        // -- Get the banks --
        for (Item item : tables.bankTable.scan()) {
            result.add(createBankFromDynamoDBItem(item));
        }
        // -- Sort by name --
        Collections.sort(result, new Comparator<Bank>() {
            @Override
            public int compare(Bank bank1, Bank bank2) {
                return bank1.getBankName().compareTo(bank2.getBankName());
            }
        });
        // -- Return the result --
        return result;
    }

    @Override
    public List<PrettyPrintingDate> getAllowedDates() throws SQLException {
        List<PrettyPrintingDate> result = new ArrayList<PrettyPrintingDate>();
        for (Item scanOutcome : tables.allowedDatesTable.scan()) {
            String dateStr = scanOutcome.getString(event_date_allowed.name());
            try {
                result.add(PrettyPrintingDate.fromParsableDate(dateStr));
            }
            catch(ParseException err) {
                throw new RuntimeException("Invalid date in the database: '" + dateStr + "'.", err);
            }
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public List<String> getAllowedTimes() throws SQLException {
        List<TimeAndSortKey> sortableTimes = new ArrayList<TimeAndSortKey>();
        for (Item scanOutcome : tables.allowedTimesTable.scan()) {
            sortableTimes.add(new TimeAndSortKey(
                    scanOutcome.getString(event_time_allowed.name()),
                    scanOutcome.getInt(event_time_sort_key.name())));
        }
        Collections.sort(sortableTimes);
        List<String> result = new ArrayList<String>(sortableTimes.size());
        for (TimeAndSortKey sortableTime : sortableTimes) {
            result.add(sortableTime.timeStr);
        }
        return result;
    }

    @Override
    public void deleteSchool(String schoolId) throws SQLException, NoSuchSchoolException {
        // Note: Does NOT verify whether the school exists and throw NoSuchSchoolException where appropriate
        tables.schoolTable.deleteItem(new PrimaryKey(school_id.name(), schoolId));
    }

    @Override
    public void deleteBank(String bankId) throws SQLException, NoSuchBankException {
        // Does not verify that the bank exists and throw NoSuchBankException
        // FIXME: Does not currently delete the bank admin and all volunteers
        tables.bankTable.deleteItem(new PrimaryKey(bank_id.name(), bankId));
    }

    @Override
    public void deleteVolunteer(String volunteerId) throws SQLException, NoSuchUserException, VolunteerHasEventsException {
        delegate.deleteVolunteer(volunteerId); // FIXME: User Related
    }

    @Override
    public void deleteTeacher(String teacherId) throws SQLException, NoSuchUserException, TeacherHasEventsException {
        // FIXME: Needs to validate that the teacher has no events and raise an exception if it does.
        tables.userTable.deleteItem(new PrimaryKey(user_id.name(), teacherId));
    }

    @Override
    public void deleteEvent(String eventId) throws SQLException, NoSuchEventException {
        delegate.deleteEvent(eventId);
    }

    @Override
    public List<Event> getAllEvents() throws SQLException, InconsistentDatabaseException {
        return delegate.getAllEvents();
    }

    @Override
    public Event getEventById(String eventId) throws SQLException {
        return delegate.getEventById(eventId);
    }

    @Override
    public void modifySchool(EditSchoolFormData school) throws SQLException, NoSuchSchoolException {
        // This approach will CREATE the school if it doesn't exist. I THINK that behavior doesn't break anything.
        tables.schoolTable.updateItem(
                new PrimaryKey(school_id.name(), school.getSchoolId()),
                attributeUpdate(school_name, school.getSchoolName()),
                attributeUpdate(school_addr1, school.getSchoolAddress1()),
                attributeUpdate(school_city, school.getCity()),
                attributeUpdate(school_state, school.getState()),
                attributeUpdate(school_zip, school.getZip()),
                attributeUpdate(school_county, school.getCounty()),
                attributeUpdate(school_district, school.getDistrict()),
                attributeUpdate(school_phone, school.getPhone()),
                attributeUpdate(school_lmi_eligible, school.getLmiEligible()),
                attributeUpdate(school_slc, school.getSLC()));
    }

    @Override
    public void insertNewBankAndAdmin(CreateBankFormData formData) throws SQLException, EmailAlreadyInUseException {
        // FIXME: Only does bank for now, and NOT admin, because that table doesn't exist yet.
        Item item = new Item()
                .withPrimaryKey(bank_id.name(), createUniqueId())
                .withString(bank_name.name(), formData.getBankName());
        tables.bankTable.putItem(item);
    }

    @Override
    public void modifyBankAndBankAdmin(EditBankFormData formData) throws SQLException, EmailAlreadyInUseException, NoSuchBankException {
        // FIXME: Only does bank for now, and NOT admin, because that table doesn't exist yet.
        // This approach will CREATE the bank if it doesn't exist. I THINK that behavior doesn't break anything.
        tables.bankTable.updateItem(
                new PrimaryKey(bank_id.name(), formData.getBankId()),
                attributeUpdate(bank_name, formData.getBankName()),
                attributeUpdate(min_lmi_for_cra, formData.getMinLMIForCRA()));
    }

    @Override
    public void setBankSpecificFieldLabel(SetBankSpecificFieldLabelFormData formData) throws SQLException, NoSuchBankException {
        // This approach will CREATE the bank if it doesn't exist. I THINK that behavior doesn't break anything.
        tables.bankTable.updateItem(
                new PrimaryKey(bank_id.name(), formData.getBankId()),
                attributeUpdate(bank_specific_data_label, formData.getBankSpecificFieldLabel()));
    }

    @Override
    public void insertNewSchool(CreateSchoolFormData school) throws SQLException {
        Item item = new Item()
                .withPrimaryKey(school_id.name(), createUniqueId())
                .withString(school_name.name(), school.getSchoolName())
                .withString(school_addr1.name(), school.getSchoolAddress1())
                .withString(school_city.name(), school.getCity())
                .withString(school_state.name(), school.getState())
                .withString(school_zip.name(), school.getZip())
                .withString(school_county.name(), school.getCounty())
                .withString(school_district.name(), school.getDistrict())
                .withString(school_phone.name(), school.getPhone())
                .withString(school_lmi_eligible.name(), school.getLmiEligible())
                .withString(school_slc.name(), school.getSLC());
        tables.schoolTable.putItem(item);
    }

    @Override
    public void insertNewAllowedDate(AddAllowedDateFormData formData) throws SQLException, AllowedDateAlreadyInUseException {
        tables.allowedDatesTable.putItem(new Item()
                .withPrimaryKey(event_date_allowed.name(), formData.getParsableDateStr()));
    }

    @Override
    public void insertNewAllowedTime(AddAllowedTimeFormData formData) throws SQLException, AllowedTimeAlreadyInUseException, NoSuchAllowedTimeException {
        // -- Get the existing list of times so we can ensure they are properly sorted --
        List<String> allowedTimes = getAllowedTimes();
        // -- Make sure it's OK to insert --
        if (!formData.getTimeToInsertBefore().isEmpty() && !allowedTimes.contains(formData.getTimeToInsertBefore())) {
            throw new NoSuchAllowedTimeException();
        }
        if (allowedTimes.contains(formData.getAllowedTime())) {
            throw new AllowedTimeAlreadyInUseException();
        }
        // -- Delete existing values from the database --
        // NOTE: not even slightly threadsafe. Won't be a problem in practice.
        for (String allowedTime : allowedTimes) {
            deleteAllowedTime(allowedTime);
        }
        // -- Now insert the new values --
        int sortKey = 0;
        for (String allowedTime : allowedTimes) {
            if (!formData.getTimeToInsertBefore().isEmpty() && formData.getTimeToInsertBefore().equals(allowedTime)) {
                // - Now we insert the new one -
                tables.allowedTimesTable.putItem(new Item()
                        .withPrimaryKey(event_time_allowed.name(), formData.getAllowedTime())
                        .withInt(event_time_sort_key.name(), sortKey));
                sortKey += 1;
            }
            // - Now we insert the one from the list -
            tables.allowedTimesTable.putItem(new Item()
                    .withPrimaryKey(event_time_allowed.name(), allowedTime)
                    .with(event_time_sort_key.name(), sortKey));
            sortKey += 1;
        }
        if (formData.getTimeToInsertBefore().isEmpty()) {
            // - Add the new one at the end -
            tables.allowedTimesTable.putItem(new Item()
                    .withPrimaryKey(event_time_allowed.name(), formData.getAllowedTime())
                    .with(event_time_sort_key.name(), sortKey));
        }
    }

    @Override
    public void modifyEvent(EventRegistrationFormData formData) throws SQLException, NoSuchEventException {
        delegate.modifyEvent(formData);
    }

    @Override
    public void updateUserCredential(String userId, String hashedPassword, String salt) throws SQLException {
        // This approach will CREATE the user if it doesn't exist. I THINK that behavior doesn't break anything.
        tables.userTable.updateItem(
                new PrimaryKey(user_id.name(), userId),
                attributeUpdate(user_hashed_password, hashedPassword),
                attributeUpdate(user_password_salt, salt));
    }

    @Override
    public void updateResetPasswordToken(String userId, String resetPasswordToken) throws SQLException {
        // This approach will CREATE the user if it doesn't exist. I THINK that behavior doesn't break anything.
        tables.userTable.updateItem(
                new PrimaryKey(user_id.name(), userId),
                attributeUpdate(user_reset_password_token, resetPasswordToken));
    }

    @Override
    public void updateApprovalStatusById(String volunteerId, ApprovalStatus approvalStatus) throws SQLException {
        // This approach will CREATE the user if it doesn't exist. I THINK that behavior doesn't break anything.
        tables.userTable.updateItem(
                new PrimaryKey(user_id.name(), volunteerId),
                intAttributeUpdate(user_approval_status, approvalStatus.getDbValue()));
    }

    @Override
    public void deleteAllowedTime(String time) throws SQLException, NoSuchAllowedTimeException {
        tables.allowedTimesTable.deleteItem(new PrimaryKey(event_time_allowed.name(), time));
    }

    @Override
    public void deleteAllowedDate(PrettyPrintingDate date) throws SQLException, NoSuchAllowedDateException {
        tables.allowedDatesTable.deleteItem(new PrimaryKey(event_date_allowed.name(), date.getParseable()));
    }

    @Override
    public SiteStatistics getSiteStatistics() throws SQLException {
        return delegate.getSiteStatistics();
    }

    @Override
    public List<Teacher> getTeacherWithSchoolData() throws SQLException {
        return delegate.getTeacherWithSchoolData();
    }

    @Override
    public List<Teacher> getTeachersBySchool(String schoolId) throws SQLException {
        // NOTE: This is a very rare operation (only used for deleting a school) so there is
        // no need for efficiency. Therefore we will NOT use an index, but a full table scan.
        List<Teacher> result = new ArrayList<Teacher>();
        for (Item item : tables.userTable.scan()) {
            UserType userType = UserType.fromDBValue(item.getString(user_type.name()));
            String organizationId = item.getString(user_organization_id.name());
            if (userType == UserType.TEACHER && organizationId.equals(schoolId)) {
                result.add((Teacher) createUserFromDynamoDBItem(item));
            }
        }
        return result;
    }

    @Override
    public List<Volunteer> getVolunteersWithBankData() throws SQLException {
        return delegate.getVolunteersWithBankData();
    }

    @Override
    public List<Teacher> getMatchedTeachers() throws SQLException {
        return delegate.getMatchedTeachers();
    }

    @Override
    public List<Teacher> getUnMatchedTeachers() throws SQLException {
        return delegate.getUnMatchedTeachers();
    }

    @Override
    public List<Volunteer> getMatchedVolunteers() throws SQLException {
        return delegate.getMatchedVolunteers();
    }

    @Override
    public List<Volunteer> getUnMatchedVolunteers() throws SQLException {
        return delegate.getUnMatchedVolunteers();
    }

    @Override
    public List<BankAdmin> getBankAdmins() throws SQLException {
        return delegate.getBankAdmins(); // FIXME: User Related
    }

    @Override
    public Map<String, String> getSiteSettings() throws SQLException {
        Map<String,String> result = new HashMap<String,String>();
        for (Item scanOutcome : tables.siteSettingsTable.scan()) {
            result.put(
                    scanOutcome.getString(site_setting_name.name()),
                    scanOutcome.getString(site_setting_value.name()));
        }
        return result;
    }

    @Override
    public void modifySiteSetting(String settingName, String settingValue) throws SQLException {
        tables.siteSettingsTable.putItem(new Item()
                .withPrimaryKey(site_setting_name.name(), settingName)
                .withString(site_setting_value.name(), settingValue));
    }
}
