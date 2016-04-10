package ir;

/**
 * Created by ted-cassirer on 2/9/16.
 */

//So we can add all information to a list before writing it to the file
public class Quartet{
    public final Integer docID;
    public final String token;
    public final Integer offset;

    Quartet(Integer docID, String token, Integer offset){
        this.docID = docID;
        this.token = token;
        this.offset = offset;
    }
}
