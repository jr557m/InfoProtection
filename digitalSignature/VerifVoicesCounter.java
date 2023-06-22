import java.util.HashMap;

class VerifVoicesCounter {
    private HashMap<String, Integer> votesHashMap;

    public VerifVoicesCounter() {
        this.votesHashMap = new HashMap<String, Integer>();
    }

    public void incrementCount(String candidate) {
        if (votesHashMap.containsKey(candidate)) {
            votesHashMap.put(candidate, votesHashMap.get(candidate) + 1);
        } else {
            votesHashMap.put(candidate, 1);
        }
    };

    public HashMap<String, Integer> getCounter() {
        return this.votesHashMap;
    };
}