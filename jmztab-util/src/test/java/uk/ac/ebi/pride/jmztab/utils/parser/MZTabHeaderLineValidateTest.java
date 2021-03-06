package uk.ac.ebi.pride.jmztab.utils.parser;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.jmztab.model.MZTabDescription;
import uk.ac.ebi.pride.jmztab.model.Metadata;
import uk.ac.ebi.pride.jmztab.utils.errors.FormatErrorType;
import uk.ac.ebi.pride.jmztab.utils.errors.LogicalErrorType;
import uk.ac.ebi.pride.jmztab.utils.errors.MZTabErrorList;
import uk.ac.ebi.pride.jmztab.utils.errors.MZTabException;

import java.io.FileNotFoundException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

/**
 * @author qingwei
 * @since 10/09/13
 */
public class MZTabHeaderLineValidateTest {
    private static Logger logger = LoggerFactory.getLogger(MZTabHeaderLineValidateTest.class);

    private Metadata metadata;
    private MZTabErrorList errorList;

    @Before
    public void setUp() throws Exception {
        MTDLineParserTest test = new MTDLineParserTest();
        String fileName = "testset/mtdFile.txt";

        URL uri = MZTabHeaderLineValidateTest.class.getClassLoader().getResource(fileName);
        if(uri!=null) {
            metadata = test.parseMetadata(uri.getFile());
        } else {
            throw new FileNotFoundException(fileName);
        }
        errorList = new MZTabErrorList();
    }

    @Test
    public void testStableColumns() throws Exception {
        // miss accession column.
        String prh = "PRH\tdescription\ttaxid\tspecies\tdatabase\tdatabase_version\t" +
            "search_engine\tambiguity_members\tmodifications";

        PRHLineParser prhParser = new PRHLineParser(metadata);
        try {
            prhParser.parse(1, prh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == FormatErrorType.StableColumn);
            logger.debug(e.getMessage());
        }

        // miss retention_time_window column
        String peh = "PEH\tsequence\taccession\tunique\tdatabase\tdatabase_version\tsearch_engine\t" +
            "modifications\tretention_time\tcharge\tmass_to_charge\tspectra_ref";

        PEHLineParser pehParser = new PEHLineParser(metadata);
        try {
            pehParser.parse(1, peh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == FormatErrorType.StableColumn);
            logger.debug(e.getMessage());
        }
    }

//    @Test
//    public void testFixedAndVariableMod() throws Exception {
//        // if PSM section is present, fixed_mod and variable_mod should be defined in metadata.
//        String psh = "PSH\tsequence\tPSM_ID\taccession\tunique\tdatabase\tdatabase_version\t" +
//            "search_engine\tsearch_engine_score\treliability\tmodifications\tretention_time\tcharge\t" +
//            "exp_mass_to_charge\tcalc_mass_to_charge\turi\tspectra_ref\tpre\tpost\tstart\tend";
//        PSHLineParser pshParser = new PSHLineParser(metadata);
//
//        try {
//            pshParser.parse(1, psh, errorList);
//            assertTrue(true);
//        } catch (MZTabException e) {
//            assertTrue(false);
//        }
//
//        metadata.getFixedModMap().clear();
//        try {
//            pshParser.parse(1, psh, errorList);
//            assertTrue(false);
//        } catch (MZTabException e) {
//            assertTrue(e.getError().getType() == LogicalErrorType.FixedMod);
//            logger.debug(e.getMessage());
//        }
//    }

    @Test
    public void testMsRunOptionalColumn() throws Exception {
        // not define ms_run[30] in metadata.
        String prh = "PRH\taccession\tdescription\ttaxid\tspecies\tdatabase\tdatabase_version\tsearch_engine\t" +
            "best_search_engine_score[1]\tsearch_engine_score[1]_ms_run[1]\treliability\tnum_psms_ms_run[1]\t" +
            "num_psms_ms_run[30]\tnum_peptides_distinct_ms_run[1]\tnum_peptides_distinct_ms_run[2]\t" +
            "num_peptides_unique_ms_run[1]\tambiguity_members\tmodifications\turi\tgo_terms\tprotein_coverage";

        PRHLineParser prhParser = new PRHLineParser(metadata);
        try {
            prhParser.parse(1, prh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == LogicalErrorType.MsRunNotDefined);
            logger.debug(e.getMessage());
        }

        // num_peptides_distinct_ms_run[1] not allowed displayed in the peptide header.
        String peh = "PEH\tsequence\taccession\tunique\tdatabase\tdatabase_version\tsearch_engine\t" +
            "best_search_engine_score[1]\tnum_peptides_distinct_ms_run[1]\treliability\tmodifications\t" +
            "retention_time\tretention_time_window\tcharge\tmass_to_charge\turi\tspectra_ref\n";

        PEHLineParser pehParser = new PEHLineParser(metadata);
        try {
            pehParser.parse(1, peh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() ==  LogicalErrorType.ColumnNotValid);
            logger.debug(e.getMessage());
        }

        // search_engine_score_ms_run[0] not validat number.
        String smh = "SMH\tidentifier\tchemical_formula\tsmiles\tinchi_key\tdescription\texp_mass_to_charge\t" +
            "calc_mass_to_charge\tcharge\tretention_time\ttaxid\tspecies\tdatabase\tdatabase_version\t" +
            "reliability\turi\tspectra_ref\tsearch_engine\tbest_search_engine_score[1]\tsearch_engine_score[1]_ms_run[0]\t" +
            "modifications";

        SMHLineParser smhParser = new SMHLineParser(metadata);
        try {
            smhParser.parse(1, smh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == LogicalErrorType.IdNumber);
            logger.debug(e.getMessage());
        }
    }

    @Test
    public void testAbundanceAssayColumns() throws Exception {
        String prh = "PRH\taccession\tdescription\ttaxid\tspecies\tdatabase\tdatabase_version\t" +
            "search_engine\tbest_search_engine_score[1]\treliability\tambiguity_members\tmodifications\turi\t" +
            "go_terms\tprotein_coverage";

        // peptide_abundance_assay not allow defined in the protein header.
        prh += "\tpeptide_abundance_assay[1]";
        PRHLineParser prhParser = new PRHLineParser(metadata);
        try {
            prhParser.parse(1, prh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == FormatErrorType.AbundanceColumn);
            logger.debug(e.getMessage());
        }



        prh = "PRH\taccession\tdescription\ttaxid\tspecies\tdatabase\tdatabase_version\t" +
            "search_engine\tbest_search_engine_score[1]\treliability\tambiguity_members\tmodifications\turi\t" +
            "go_terms\tprotein_coverage";

        // assay[30] not defined in the metadata.
        prh += "\tprotein_abundance_assay[30]";
        prhParser = new PRHLineParser(metadata);
        try {
            prhParser.parse(1, prh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == LogicalErrorType.AssayNotDefined);
            logger.debug(e.getMessage());
        }


        String psh = "PSH\tsequence\tPSM_ID\taccession\tunique\tdatabase\tdatabase_version\t" +
            "search_engine\tsearch_engine_score[1]\treliability\tmodifications\tretention_time\tcharge\t" +
            "exp_mass_to_charge\tcalc_mass_to_charge\turi\tspectra_ref\tpre\tpost\tstart\tend";

        // PSM header doesn't support optional columns.
        psh += "\tpsm_abundance_assay[1]";
        PSHLineParser pshParser = new PSHLineParser(metadata);
        try {
            pshParser.parse(1, psh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == LogicalErrorType.ColumnNotValid);
            logger.debug(e.getMessage());
        }
    }

    @Test
    public void testAbundanceStudyVariableColumns() throws Exception {
        String prh = "PRH\taccession\tdescription\ttaxid\tspecies\tdatabase\tdatabase_version\t" +
            "search_engine\tbest_search_engine_score[1]\treliability\tambiguity_members\tmodifications\turi\t" +
            "go_terms\tprotein_coverage";

        // study_variable[10] not defined in the metadata.
        prh += "\tprotein_abundance_study_variable[10]\tprotein_abundance_stdev_study_variable[10]\tprotein_abundance_std_error_study_variable[10]";
        PRHLineParser prhParser = new PRHLineParser(metadata);
        try {
            prhParser.parse(1, prh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == LogicalErrorType.StudyVariableNotDefined);
            logger.debug(e.getMessage());
        }

        // miss protein_abundance_stdev_study_variable[1], three columns should be display together.
        prh = "PRH\taccession\tdescription\ttaxid\tspecies\tdatabase\tdatabase_version\t" +
            "search_engine\tbest_search_engine_score[1]\treliability\tambiguity_members\tmodifications\turi\t" +
            "go_terms\tprotein_coverage";
        prh += "\tprotein_abundance_study_variable[1]\tprotein_abundance_std_error_study_variable[1]";
        prhParser = new PRHLineParser(metadata);
        try {
            prhParser.parse(1, prh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == LogicalErrorType.AbundanceColumnTogether);
            logger.debug(e.getMessage());
        }

        // study_variable id number not same error.
        prh = "PRH\taccession\tdescription\ttaxid\tspecies\tdatabase\tdatabase_version\t" +
            "search_engine\tbest_search_engine_score[1]\treliability\tambiguity_members\tmodifications\turi\t" +
            "go_terms\tprotein_coverage";
        prh += "\tprotein_abundance_study_variable[1]\tprotein_abundance_stdev_study_variable[2]\tprotein_abundance_std_error_study_variable[1]";
        prhParser = new PRHLineParser(metadata);
        try {
            prhParser.parse(1, prh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == LogicalErrorType.AbundanceColumnSameId);
            logger.debug(e.getMessage());
        }
    }

    @Test
    public void testCVParamOptionalColumn() throws Exception {
        //  Column names MUST only contain the following characters: 'A'-'Z', 'a'-'z', '0'-'9', '_', '-', '[', ']', and ':'.
        String prh = "PRH\taccession\tdescription\ttaxid\tspecies\tdatabase\tdatabase_version\t" +
            "search_engine\tbest_search_engine_score[1]\treliability\tambiguity_members\tmodifications\turi\t" +
            "go_terms\tprotein_coverage";
        prh += "\topt_assay[1]_#y_value";
        PRHLineParser prhParser = new PRHLineParser(metadata);
        try {
            prhParser.parse(1, prh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == FormatErrorType.OptionalCVParamColumn);
            logger.debug(e.getMessage());
        }

        //  assay[10] not defined in the metadata.
        prh = "PRH\taccession\tdescription\ttaxid\tspecies\tdatabase\tdatabase_version\t" +
            "search_engine\tbest_search_engine_score[1]\treliability\tambiguity_members\tmodifications\turi\t" +
            "go_terms\tprotein_coverage";
        prh += "\topt_assay[10]_my_value";
        prhParser = new PRHLineParser(metadata);
        try {
            prhParser.parse(1, prh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == LogicalErrorType.AssayNotDefined);
            logger.debug(e.getMessage());
        }

        //  assay[x] is not a number.
        prh = "PRH\taccession\tdescription\ttaxid\tspecies\tdatabase\tdatabase_version\t" +
            "search_engine\tbest_search_engine_score[1]\treliability\tambiguity_members\tmodifications\turi\t" +
            "go_terms\tprotein_coverage";
        prh += "\topt_assay[x]_my_value";
        prhParser = new PRHLineParser(metadata);
        try {
            prhParser.parse(1, prh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == LogicalErrorType.IdNumber);
            logger.debug(e.getMessage());
        }
    }

    @Test
    public void testModeType() throws Exception {
        metadata.setMZTabMode(MZTabDescription.Mode.Complete);
        metadata.setMZTabType(MZTabDescription.Type.Identification);

        // In Complete and Identification document, user should provide:
        // search_engine_score_ms_run[1-n], num_psms_ms_run[1-n], num_peptides_distinct_ms_run[1-n]
        // num_peptide_unique_ms_run[1-n]
        String prh = "PRH\taccession\tdescription\ttaxid\tspecies\tdatabase\tdatabase_version\t" +
            "search_engine\tbest_search_engine_score[1]\treliability\tambiguity_members\tmodifications\turi\t" +
            "go_terms\tprotein_coverage";
        PRHLineParser prhParser = new PRHLineParser(metadata);
        try {
            prhParser.parse(1, prh, errorList);
            assertTrue(false);
        } catch (MZTabException e) {
            assertTrue(e.getError().getType() == LogicalErrorType.NotDefineInHeader);
            logger.debug(e.getMessage());
        }
    }
}
