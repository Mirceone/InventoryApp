package com.mirceone.inventoryapp.routing;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.FirstSolutionStrategy;
import com.google.ortools.constraintsolver.LocalSearchMetaheuristic;
import com.google.ortools.constraintsolver.RoutingIndexManager;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.RoutingSearchParameters;
import com.google.protobuf.Duration;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Symmetric TSP with single vehicle; depot is matrix row/column index 0.
 */
@Component
public class OrToolsTspSolver {

    @PostConstruct
    void loadNative() {
        Loader.loadNativeLibraries();
    }

    /**
     * @param distanceMeters symmetric matrix, distanceMeters[i][j] >= 0
     * @return visit order **excluding** duplicated return to depot (permutation of 1..n-1 after depot 0), or indices 0..n-1 in visit order including start 0 once
     */
    public List<Integer> solveOrder(long[][] distanceMeters) {
        if (distanceMeters.length <= 1) {
            return List.of(0);
        }
        int n = distanceMeters.length;
        RoutingIndexManager manager = new RoutingIndexManager(n, 1, 0);
        RoutingModel routing = new RoutingModel(manager);

        final int transitCallbackIndex = routing.registerTransitCallback((long fromIndex, long toIndex) -> {
            int fromNode = manager.indexToNode(fromIndex);
            int toNode = manager.indexToNode(toIndex);
            return distanceMeters[fromNode][toNode];
        });
        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);

        RoutingSearchParameters searchParameters = RoutingSearchParameters.newBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
                .setTimeLimit(Duration.newBuilder().setSeconds(5).build())
                .build();

        Assignment solution = routing.solveWithParameters(searchParameters);
        if (solution == null) {
            throw new IllegalStateException("OR-Tools did not find a TSP solution");
        }

        List<Integer> order = new ArrayList<>();
        long index = routing.start(0);
        while (!routing.isEnd(index)) {
            order.add(manager.indexToNode((int) index));
            index = solution.value(routing.nextVar(index));
        }
        return order;
    }

    public long tourLengthMeters(long[][] distanceMeters, List<Integer> visitOrder) {
        if (visitOrder.size() < 2) {
            return 0;
        }
        long sum = 0;
        for (int i = 0; i < visitOrder.size(); i++) {
            int a = visitOrder.get(i);
            int b = visitOrder.get((i + 1) % visitOrder.size());
            sum += distanceMeters[a][b];
        }
        return sum;
    }
}
