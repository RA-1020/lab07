/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class SocialNetworkTest {

    /*
     * Testing strategy
     *
     * guessFollowsGraph(tweets):
     *   number of tweets:              0, 1, >1
     *   @-mentions in a tweet:         0, 1, >1, same user mentioned repeatedly
     *   tweets by the same author:     1, >1
     *   username case:                 all lower, mixed (author and/or mention)
     *   author mentions themself:      no, yes
     *
     *   The postcondition is underdetermined: an implementation MAY infer
     *   extra follows from other evidence, and a user who follows nobody MAY
     *   appear with an empty set or be absent. The tests therefore only check
     *   follows that MUST exist (Ernie @-mentions Bert => Ernie follows Bert),
     *   never the exact size of the map, and compare usernames
     *   case-insensitively through follows(...) instead of using containsKey.
     *
     * influencers(followsGraph):
     *   graph size:                    0, 1 user, >1 users
     *   users with >=1 follower:       0, 1, >1
     *   follower counts:               all distinct, some tied
     *   followsGraph:                  must not be modified
     *
     * Each test below notes which partitions it covers.
     */

    private static final Instant d1 = Instant.parse("2026-10-23T09:00:00Z");
    private static final Instant d2 = Instant.parse("2026-10-23T10:00:00Z");

    private static final Tweet tweet1 =
            new Tweet(1, "userA", "Hey @userB how are you?", d1);
    private static final Tweet tweet2 =
            new Tweet(2, "userA", "What about @userC and @userD?", d1);
    private static final Tweet tweet3 =
            new Tweet(3, "userB", "Just a tweet with no mentions.", d1);
    private static final Tweet tweet4 =
            new Tweet(4, "userD", "@userB @userB @userB", d2);
    private static final Tweet tweet5 =
            new Tweet(5, "UserE", "Shout-out to @USERB", d2);
    private static final Tweet tweet6 =
            new Tweet(6, "userF", "Reminder to myself @UserF", d2);

    @Test(expected = AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    /** True if, in graph, user a follows user b, ignoring case. */
    private static boolean follows(Map<String, Set<String>> graph,
            String a, String b) {
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(a)) {
                for (String followed : entry.getValue()) {
                    if (followed.equalsIgnoreCase(b)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** True if nobody in graph follows anybody (keys with empty sets are allowed). */
    private static boolean noFollows(Map<String, Set<String>> graph) {
        return graph.values().stream().allMatch(Set::isEmpty);
    }

    /* ---------------- guessFollowsGraph() ---------------- */

    // Covers: 0 tweets
    @Test
    public void testGuessFollowsGraphEmpty() {
        Map<String, Set<String>> followsGraph =
                SocialNetwork.guessFollowsGraph(new ArrayList<>());

        assertTrue("expected empty graph", followsGraph.isEmpty());
    }

    // Covers: 1 tweet, 0 mentions
    @Test
    public void testGuessFollowsGraphNoMentions() {
        Map<String, Set<String>> followsGraph =
                SocialNetwork.guessFollowsGraph(Arrays.asList(tweet3));

        assertTrue("expected no follows relationships", noFollows(followsGraph));
    }

    // Covers: 1 tweet, 1 mention, lower-case usernames
    @Test
    public void testGuessFollowsGraphSingleMention() {
        Map<String, Set<String>> followsGraph =
                SocialNetwork.guessFollowsGraph(Arrays.asList(tweet1));

        assertTrue("expected userA to follow userB", follows(followsGraph, "userA", "userB"));
    }

    // Covers: 1 tweet, >1 mentions
    @Test
    public void testGuessFollowsGraphMultipleMentions() {
        Map<String, Set<String>> followsGraph =
                SocialNetwork.guessFollowsGraph(Arrays.asList(tweet2));

        assertTrue("expected userA to follow userC", follows(followsGraph, "userA", "userC"));
        assertTrue("expected userA to follow userD", follows(followsGraph, "userA", "userD"));
    }

    // Covers: >1 tweets, >1 tweets by the same author, repeated mention of one user
    @Test
    public void testGuessFollowsGraphMultipleTweetsOneUser() {
        List<Tweet> tweets = Arrays.asList(tweet1, tweet2, tweet4);
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue(follows(followsGraph, "userA", "userB"));
        assertTrue(follows(followsGraph, "userA", "userC"));
        assertTrue(follows(followsGraph, "userA", "userD"));
        assertTrue(follows(followsGraph, "userD", "userB"));
    }

    // Covers: mixed-case author and mention
    @Test
    public void testGuessFollowsGraphCaseInsensitive() {
        Map<String, Set<String>> followsGraph =
                SocialNetwork.guessFollowsGraph(Arrays.asList(tweet5));

        assertTrue("usernames are case-insensitive", follows(followsGraph, "usere", "userb"));
    }

    // Covers: author mentions themself
    @Test
    public void testGuessFollowsGraphSelfMention() {
        Map<String, Set<String>> followsGraph =
                SocialNetwork.guessFollowsGraph(Arrays.asList(tweet6));

        assertFalse("users can't follow themselves", follows(followsGraph, "userF", "userF"));
    }

    // Covers: input list must not be modified
    @Test
    public void testGuessFollowsGraphDoesNotModifyInput() {
        List<Tweet> tweets = new ArrayList<>(Arrays.asList(tweet1, tweet2, tweet4));
        List<Tweet> copy = new ArrayList<>(tweets);

        SocialNetwork.guessFollowsGraph(tweets);

        assertEquals("input list was modified", copy, tweets);
    }

    /* ---------------- influencers() ---------------- */

    // Covers: graph size 0
    @Test
    public void testInfluencersEmptyGraph() {
        List<String> influencers = SocialNetwork.influencers(new HashMap<>());

        assertTrue("expected empty list", influencers.isEmpty());
    }

    // Covers: graph size 1, 0 users with followers
    @Test
    public void testInfluencersSingleUserWithoutFollowers() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("usera", new HashSet<>());

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertTrue("a user with no followers is not an influencer", influencers.isEmpty());
    }

    // Covers: >1 users, exactly 1 user with followers
    @Test
    public void testInfluencersSingleInfluencer() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("usera", new HashSet<>(Arrays.asList("userc")));
        followsGraph.put("userb", new HashSet<>(Arrays.asList("userc")));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals(Arrays.asList("userc"), influencers);
    }

    // Covers: >1 users with followers, all counts distinct
    @Test
    public void testInfluencersMultipleInfluencers() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("usera", new HashSet<>(Arrays.asList("userc", "userd", "usere")));
        followsGraph.put("userb", new HashSet<>(Arrays.asList("userc", "userd")));
        followsGraph.put("userf", new HashSet<>(Arrays.asList("userc")));
        // followers: userc = 3, userd = 2, usere = 1

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals(Arrays.asList("userc", "userd", "usere"), influencers);
    }

    // Covers: tied counts (ties may come in either order, but must sit together
    // between the strictly more- and less-followed users)
    @Test
    public void testInfluencersTiedInfluence() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("usera", new HashSet<>(Arrays.asList("userx", "usery", "userz")));
        followsGraph.put("userb", new HashSet<>(Arrays.asList("userx", "usery")));
        followsGraph.put("userc", new HashSet<>(Arrays.asList("userx")));
        followsGraph.put("userd", new HashSet<>(Arrays.asList("userw")));
        followsGraph.put("usere", new HashSet<>(Arrays.asList("userw")));
        // followers: userx = 3, usery = 2, userw = 2, userz = 1

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals(4, influencers.size());
        assertEquals("most followed comes first", "userx", influencers.get(0));
        assertEquals("tied users fill positions 1 and 2",
                new HashSet<>(Arrays.asList("usery", "userw")),
                new HashSet<>(influencers.subList(1, 3)));
        assertEquals("least followed comes last", "userz", influencers.get(3));
    }

    // Covers: followsGraph must not be modified
    @Test
    public void testInfluencersDoesNotModifyInput() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("usera", new HashSet<>(Arrays.asList("userb")));
        Map<String, Set<String>> copy = new HashMap<>();
        copy.put("usera", new HashSet<>(Collections.singletonList("userb")));

        SocialNetwork.influencers(followsGraph);

        assertEquals("input graph was modified", copy, followsGraph);
    }

    /*
     * Warning: all the tests you write here must be runnable against any
     * SocialNetwork class that follows the spec. It will be run against several
     * staff implementations of SocialNetwork, which will be done by overwriting
     * (temporarily) your version of SocialNetwork. So tests should not depend
     * on any methods you might add to SocialNetwork beyond those in its spec.
     */
}
