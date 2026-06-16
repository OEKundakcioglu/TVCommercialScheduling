package scheduling.solver;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import scheduling.model.Commercial;
import scheduling.model.Inventory;

@Getter
@RequiredArgsConstructor
public class Solution {

    private final Map<Inventory, List<Commercial>> assignments;
    private final double totalRevenue;
}
