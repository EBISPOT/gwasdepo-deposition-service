package uk.ac.ebi.spot.gwas.deposition.constants;

public class GWASDepositionBackendConstants {

    public static final String API_DAILYSTATS = "/daily-stats";

    public static final String API_PUBLICATIONS = "/publications";

    public static final String API_BODY_OF_WORK = "/bodyofwork";

    public static final String API_REMOVE_EMBARGO = "/remove-embargo";

    public static final String API_ADD_EMBARGO = "/add-embargo";

    public static final String API_BYPASS_SS_VALIDATION= "/bypass-ss-validation";

    public static final String API_SUBMISSIONS = "/submissions";

    public static final String API_STUDIES = "/studies";

    public static final String API_STUDY_ENVELOPES = "/study-envelopes";

    public static final String API_SAMPLES = "/samples";

    public static final String API_ASSOCIATIONS = "/associations";

    public static final String API_UPLOADS = "/uploads";

    public static final String API_EDIT_UPLOADS = "/uploads/edit";

    public static final String API_SUBMISSIONS_LOCK = "/lock";

    public static final String API_Javers = "/javers";

    public static final String API_DOWNLOAD = "/download";

    public static final String API_SUBMIT = "/submit";

    public static final String API_REINDEX_PUBLICATIONS = "/reindex-publications";

    public static final String API_RECREATE_TEMPLATE = "/recreate-ss-template";

    public static final String API_CLEAR_PUBLICATIONS = "/clear-publications";

    public static final String PARAM_PMID = "pmid";

    public static final String PARAM_STATUS = "status";

    public static final String PARAM_BOWID = "bowId";

    public static final String PARAM_AUTHOR = "author";

    public static final String PARAM_TITLE = "title";

    public static final String LINKS_FILES = "files";

    public static final String LINKS_STUDIES = "studies";

    public static final String LINKS_SAMPLES = "samples";

    public static final String LINKS_ASSOCIATIONS = "associations";

    public static final String LINKS_PARENT = "parent";

    public static final String FILE_TEMPLATE_EXAMPLE = "template_example.xlsx";

    public static final String PREFIX_GCST = "GCST";

    public static final String PREFIX_GCP = "GCP";

    public static final String PREFIX_LABEL = "LABEL";

    public static final String PROFILE_PRODUCTION = "prod";

    public static final String PROFILE_FALLBACK = "prod-fallback";

    public static final String PROFILE_SANDBOX = "sandbox";

    public static final String PROFILE_SANDBOX_MIGRATION = "sandbox-migration";

    public static final String VALUE_NR = "NR";

    public static final String QUEUE_NAME = "study_change";
    public static final String EXCHANGE_NAME = "study_change_exchange";
    public static final String ROUTING_KEY = "study-ingest";
}
