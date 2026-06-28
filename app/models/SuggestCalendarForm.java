package models;

import org.jetbrains.annotations.Nullable;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Constraints.Validate
public class SuggestCalendarForm implements Constraints.Validatable<List<ValidationError>> {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Constraints.Required private String title;
    @Constraints.Required private String methodology;
    private List<String> lunarVariables = new ArrayList<>();
    @Constraints.Required private String source;
    private String sourceLicense;
    private String sourceLicenseOther;
    private String originCulture;
    private String hemisphereDependence;
    private String outputGranularity;
    private String contactEmail;
    private String notes;
    private String website; // honeypot — must stay empty

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();
        if (contactEmail != null && !contactEmail.isBlank() && !EMAIL.matcher(contactEmail.trim()).matches()) {
            errors.add(new ValidationError("contactEmail", "error.email"));
        }
        return errors.isEmpty() ? null : errors;
    }

    public boolean isHoneypotTriggered() {
        return website != null && !website.isBlank();
    }

    // getters/setters
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getMethodology() { return methodology; }
    public void setMethodology(String v) { this.methodology = v; }
    public List<String> getLunarVariables() { return lunarVariables; }
    public void setLunarVariables(List<String> v) { if (v != null) this.lunarVariables = v; }
    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }
    public String getSourceLicense() { return sourceLicense; }
    public void setSourceLicense(String v) { this.sourceLicense = v; }
    public String getSourceLicenseOther() { return sourceLicenseOther; }
    public void setSourceLicenseOther(String v) { this.sourceLicenseOther = v; }
    public String getOriginCulture() { return originCulture; }
    public void setOriginCulture(String v) { this.originCulture = v; }
    public String getHemisphereDependence() { return hemisphereDependence; }
    public void setHemisphereDependence(String v) { this.hemisphereDependence = v; }
    public String getOutputGranularity() { return outputGranularity; }
    public void setOutputGranularity(String v) { this.outputGranularity = v; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String v) { this.contactEmail = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public String getWebsite() { return website; }
    public void setWebsite(String v) { this.website = v; }

    @Nullable
    public String toLogLine() {
        return "title=" + sanitize(title) + " | methodology=" + sanitize(methodology) + " | source=" + sanitize(source)
                + " | vars=" + lunarVariables + " | license=" + sanitize(sourceLicense) + " (" + sanitize(sourceLicenseOther) + ")"
                + " | origin=" + sanitize(originCulture) + " | hemisphere=" + sanitize(hemisphereDependence)
                + " | granularity=" + sanitize(outputGranularity)
                + " | email=" + sanitize(contactEmail) + " | notes=" + sanitize(notes);
    }

    private static String sanitize(String value) {
        if (value == null) return null;
        return value.replace("\r", " ").replace("\n", " ");
    }
}
