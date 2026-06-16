package scheduling.model;

import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.Arrays;
import scheduling.dto.CommercialDTO;
import scheduling.dto.InventoryDTO;
import scheduling.dto.ProblemDTO;
import scheduling.dto.RatingDTO;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;

public final class ProblemBuilder {

    private ProblemBuilder() {}

    public static Problem build(ProblemDTO dto) {
        var inventories = buildInventories(dto);
        var commercials = buildCommercials(dto);
        var hours = extractHours(inventories);
        var suitabilityData = parseSuitability(dto, commercials.length, inventories.length);
        var ratings = buildRatings(dto, inventories.length);
        var revenueMatrix =
                buildRevenueMatrix(commercials, inventories, suitabilityData.suitability, ratings);

        return new Problem(
                commercials,
                inventories,
                hours,
                suitabilityData.suitability,
                suitabilityData.attentionTypes,
                suitabilityData.suitableInventoriesFor,
                suitabilityData.suitableCommercialsFor,
                ratings,
                revenueMatrix);
    }

    @SuppressWarnings("ArrayRecordComponent")
    private record SuitabilityData(
            boolean[][] suitability,
            AttentionType[][][] attentionTypes,
            int[][] suitableInventoriesFor,
            int[][] suitableCommercialsFor) {}

    @SuppressWarnings("unchecked")
    private static SuitabilityData parseSuitability(
            ProblemDTO dto, int numCommercials, int numInventories) {
        var suitability = new boolean[numCommercials][numInventories];
        var attentionLists = new ArrayList[numCommercials][numInventories];
        var suitableInvLists = new ArrayList[numCommercials];
        var suitableCommLists = new ArrayList[numInventories];

        initializeLists(attentionLists, suitableInvLists, suitableCommLists);
        populateSuitabilityFromDto(
                dto, suitability, attentionLists, suitableInvLists, suitableCommLists);

        return buildSuitabilityData(
                suitability, attentionLists, suitableInvLists, suitableCommLists);
    }

    private static void initializeLists(
            ArrayList<AttentionType>[][] attentionLists,
            ArrayList<Integer>[] suitableInvLists,
            ArrayList<Integer>[] suitableCommLists) {
        for (int i = 0; i < attentionLists.length; i++) {
            suitableInvLists[i] = new ArrayList<>();
            for (int j = 0; j < attentionLists[i].length; j++) {
                attentionLists[i][j] = new ArrayList<>();
            }
        }
        for (int j = 0; j < suitableCommLists.length; j++) {
            suitableCommLists[j] = new ArrayList<>();
        }
    }

    private static void populateSuitabilityFromDto(
            ProblemDTO dto,
            boolean[][] suitability,
            ArrayList<AttentionType>[][] attentionLists,
            ArrayList<Integer>[] suitableInvLists,
            ArrayList<Integer>[] suitableCommLists) {
        for (var commDTO : dto.commercials()) {
            var commId = commDTO.id();
            for (var entry : commDTO.suitableInventories().entrySet()) {
                var attentionType = AttentionType.valueOf(entry.getKey());
                for (var invId : entry.getValue()) {
                    if (!suitability[commId][invId]) {
                        suitability[commId][invId] = true;
                        suitableInvLists[commId].add(invId);
                        suitableCommLists[invId].add(commId);
                    }
                    attentionLists[commId][invId].add(attentionType);
                }
            }
        }
    }

    private static SuitabilityData buildSuitabilityData(
            boolean[][] suitability,
            ArrayList<AttentionType>[][] attentionLists,
            ArrayList<Integer>[] suitableInvLists,
            ArrayList<Integer>[] suitableCommLists) {
        var attentionTypes = convertAttentionTypes(attentionLists);
        var suitableInventoriesFor = convertToIntArrays(suitableInvLists);
        var suitableCommercialsFor = convertToIntArrays(suitableCommLists);

        return new SuitabilityData(
                suitability, attentionTypes, suitableInventoriesFor, suitableCommercialsFor);
    }

    private static AttentionType[][][] convertAttentionTypes(
            ArrayList<AttentionType>[][] attentionLists) {
        var result = new AttentionType[attentionLists.length][][];
        for (int i = 0; i < attentionLists.length; i++) {
            result[i] = new AttentionType[attentionLists[i].length][];
            for (int j = 0; j < attentionLists[i].length; j++) {
                result[i][j] = attentionLists[i][j].toArray(new AttentionType[0]);
            }
        }
        return result;
    }

    private static int[][] convertToIntArrays(ArrayList<Integer>[] lists) {
        var result = new int[lists.length][];
        for (int i = 0; i < lists.length; i++) {
            result[i] = Ints.toArray(lists[i]);
        }
        return result;
    }

    private static double[][][] buildRevenueMatrix(
            Commercial[] commercials,
            Inventory[] inventories,
            boolean[][] suitability,
            double[][][] ratings) {
        var revenueMatrix = new double[commercials.length][inventories.length][];

        for (var comm : commercials) {
            for (var inv : inventories) {
                if (!suitability[comm.getId()][inv.getId()]) {
                    revenueMatrix[comm.getId()][inv.getId()] = new double[0];
                    continue;
                }
                revenueMatrix[comm.getId()][inv.getId()] = buildRevenueRow(comm, inv, ratings);
            }
        }

        return revenueMatrix;
    }

    private static double[] buildRevenueRow(Commercial comm, Inventory inv, double[][][] ratings) {
        var row = new double[inv.getDuration()];

        for (var startTime = 0; startTime < inv.getDuration(); startTime++) {
            var minute = startTime / 60 + 1;
            if (minute < ratings[inv.getId()].length
                    && comm.getAudienceType() < ratings[inv.getId()][minute].length) {
                var rating = ratings[inv.getId()][minute][comm.getAudienceType()];
                row[startTime] = comm.getRevenue(rating);
            }
        }

        return row;
    }

    private static double[][][] buildRatings(ProblemDTO dto, int numInventories) {
        var maxMinute = dto.ratings().stream().mapToInt(RatingDTO::minute).max().orElse(0);
        var maxAudienceType =
                dto.ratings().stream().mapToInt(RatingDTO::audienceType).max().orElse(0);

        var ratings = new double[numInventories][maxMinute + 1][maxAudienceType + 1];

        for (var ratingDTO : dto.ratings()) {
            ratings[ratingDTO.inventoryId()][ratingDTO.minute()][ratingDTO.audienceType()] =
                    ratingDTO.rating();
        }

        return ratings;
    }

    private static Inventory[] buildInventories(ProblemDTO dto) {
        var maxId = dto.inventories().stream().mapToInt(InventoryDTO::id).max().orElse(-1);
        var inventories = new Inventory[maxId + 1];

        for (var invDTO : dto.inventories()) {
            inventories[invDTO.id()] =
                    new Inventory(
                            invDTO.id(),
                            invDTO.duration(),
                            invDTO.hour(),
                            invDTO.maxNumberOfCommercial());
        }

        return inventories;
    }

    private static Commercial[] buildCommercials(ProblemDTO dto) {
        var maxId = dto.commercials().stream().mapToInt(CommercialDTO::id).max().orElse(-1);
        var commercials = new Commercial[maxId + 1];

        for (var commDTO : dto.commercials()) {
            commercials[commDTO.id()] =
                    new Commercial(
                            commDTO.id(),
                            commDTO.group(),
                            commDTO.audienceType(),
                            commDTO.duration(),
                            commDTO.price(),
                            PricingType.valueOf(commDTO.pricingType()));
        }

        return commercials;
    }

    private static int[] extractHours(Inventory[] inventories) {
        return Arrays.stream(inventories)
                .mapToInt(Inventory::getHour)
                .distinct()
                .sorted()
                .toArray();
    }
}
