package team.eight;

/**
 * Created by ice-rock on 3/3/16.
 */
public class TermDistance implements Comparable<TermDistance>{
    private String term;
    private Double distance;

    public TermDistance(String term, double distance){
        this.term = term;
        this.distance = distance;
    }

    void setTerm(String term)
    {
        this.term = term;
    }

    void setDistance(double distance)
    {
        this.distance = distance;
    }

    String get_term(){
        return term;
    }

    double get_distance(){
        return distance;
    }

    @Override
    public int compareTo(TermDistance other) {
        return -1 * this.distance.compareTo(other.distance);
    }

    @Override
    public String toString()
    {
        return padRight(this.term + ": ", 30) + this.distance;
    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }
}
