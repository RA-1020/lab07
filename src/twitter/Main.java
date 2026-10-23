package twitter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the main program.
 * 
 * You may change this class if you wish, but you don't have to.
 */
public class Main {
    
    /**
     * URL of a server that produces a list of tweets sampled from Twitter
     * within the last hour. This server may take up to a minute to respond, if
     * it has to refresh its cached sample of tweets.
     */
    public static final URL SAMPLE_SERVER = makeURLAssertWellFormatted("http://courses.csail.mit.edu/6.005/ps1_tweets/tweetPoll.py");
    
    /**
     * @return a small hard-coded list of tweets used when SAMPLE_SERVER is unreachable
     */
    private static List<Tweet> offlineSample() {
        final Instant t = Instant.parse("2026-10-23T09:00:00Z");
        return Arrays.asList(
                new Tweet(1, "alyssa", "Reading @bbitdiddle's notes on #6005 before lab", t),
                new Tweet(2, "bbitdiddle", "@alyssa @evalu the spec says iff, not if", t.plusSeconds(60)),
                new Tweet(3, "evalu", "Agree with @bbitdiddle, and @alyssa found the bug", t.plusSeconds(120)),
                new Tweet(4, "lem_e", "Test first! cc @alyssa @bbitdiddle", t.plusSeconds(180)),
                new Tweet(5, "cy_d", "@Alyssa how many tests did you write?", t.plusSeconds(240)),
                new Tweet(6, "louis", "Note to self @louis: run JUnit with -ea", t.plusSeconds(300)),
                new Tweet(7, "louis", "@cy_d partitions > exhaustive tests", t.plusSeconds(360)),
                new Tweet(8, "ben", "Mail me at ben@mit.edu, no mention here", t.plusSeconds(420)));
    }

    private static URL makeURLAssertWellFormatted(String urlString) {
        try {
            return new URL(urlString);
        } catch (MalformedURLException murle) {
            throw new AssertionError(murle);
        }
    }
    
    /**
     * Main method of the program. Fetches a sample of tweets and prints some
     * facts about it.
     * 
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            assert false;
            throw new Error("Always run main and tests with assertions enabled");
        } catch (AssertionError ae) { }
        
        List<Tweet> tweets;
        try {
            tweets = TweetReader.readTweetsFromWeb(SAMPLE_SERVER);
        } catch (IOException ioe) {
            // The MIT sample server may be unreachable; fall back to a small
            // offline sample so the program can still be demonstrated.
            System.err.println("could not reach " + SAMPLE_SERVER + " (" + ioe.getMessage()
                    + "), using offline sample tweets");
            tweets = offlineSample();
        }
        
        // display some characteristics about the tweets
        System.err.println("fetched " + tweets.size() + " tweets");
        
        final Timespan span = Extract.getTimespan(tweets);
        System.err.println("ranging from " + span.getStart() + " to " + span.getEnd());
        
        final Set<String> mentionedUsers = Extract.getMentionedUsers(tweets);
        System.err.println("covers " + mentionedUsers.size() + " Twitter users");
        
        // infer the follows graph
        final Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);
        System.err.println("follows graph has " + followsGraph.size() + " nodes");
        
        // print the top-N influencers
        final int count = 10;
        final List<String> influencers = SocialNetwork.influencers(followsGraph);
        for (String username : influencers.subList(0, Math.min(count, influencers.size()))) {
            System.out.println(username);
        }
    }
    
}
