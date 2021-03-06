package org.campagnelab.dl.genotype.tools;

import org.campagnelab.dl.framework.tools.ShowArguments;
import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;

import java.util.function.Function;

/**
 * Arguments for ShowSomatic.
 */
public class GenotypeShowArguments extends ShowArguments {
    @Override
    protected String defaultReportType() {
        return SomaticShowReportTypes.POSITIONS.toString();
    }

    public Function<BaseInformationRecords.BaseInformation, String> getConverter() {

        switch (SomaticShowReportTypes.valueOf(reportType)) {
            case PROTOBUFF:
                return showProtobuff;
            case COUNTS:
                return showFormattedCounts;
            case POSITIONS:
            default:
                return showPositions;
        }
    }

    public enum SomaticShowReportTypes {
        PROTOBUFF,
        POSITIONS,
        COUNTS,
    }

    Function<BaseInformationRecords.BaseInformation, String> showFormattedCounts = new Function<BaseInformationRecords.BaseInformation, String>() {
        @Override
        public String apply(BaseInformationRecords.BaseInformation record) {
            StringBuffer formattedCounts = new StringBuffer();
            for (BaseInformationRecords.SampleInfo sample : record.getSamplesList()) {
                formattedCounts.append(sample.getFormattedCounts() + "\t");
            }
            return formattedCounts.toString()+ "\t"+record.getTrueGenotype();
        }
    };

    Function<BaseInformationRecords.BaseInformation, String> showPositions = new Function<BaseInformationRecords.BaseInformation, String>() {
        @Override
        public String apply(BaseInformationRecords.BaseInformation baseInformation) {
            String refId = baseInformation.hasReferenceId() ? baseInformation.getReferenceId() :
                    Integer.toString(baseInformation.getReferenceIndex());
            return String.format("%s\t%d\t%s", refId, baseInformation.getPosition(),baseInformation.getTrueGenotype());
        }
    };

    Function<BaseInformationRecords.BaseInformation, String> showProtobuff = new Function<BaseInformationRecords.BaseInformation, String>() {
        @Override
        public String apply(BaseInformationRecords.BaseInformation baseInformation) {
            return baseInformation.toString();
        }
    };
}
