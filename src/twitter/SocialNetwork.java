/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SocialNetwork provides methods that operate on a social network.
 *
 * A social network is represented by a Map<String, Set<String>> where map[A] is
 * the set of people that person A follows on Twitter, and all people are
 * represented by their Twitter usernames. Users can't follow themselves. If A
 * doesn't follow anybody, then map[A] may be the empty set, or A may not even
 * exist as a key in the map. Twitter usernames are not case sensitive, so
 * "ernie" is the same as "ERNie"; this implementation stores every username in
 * lower case so that each user appears at most once as a key or in any set.
 */
public class SocialNetwork {

    /**
     * Guess who might follow whom from a list of tweets.
     *
     * @param tweets
     *            a list of tweets providing evidence of follows relationships,
     *            not modified by this method.
     * @return a social network (a map where keys are usernames and values are
     *         sets of usernames) representing inferred follows relationships.
     *         A user A is in the set for user B if B is inferred to follow A
     *         (based on B mentioning A in a tweet).
     */
    public static Map<String, Set<String>> guessFollowsGraph(List<Tweet> tweets) {
        Map<String, Set<String>> followsGraph = new HashMap<>();

        for (Tweet tweet : tweets) {
            String author = tweet.getAuthor().toLowerCase();

            // Reuse Extract (Problem 1) instead of duplicating its regex.
            // getMentionedUsers already returns lower-case usernames.
            Set<String> mentioned =
                    Extract.getMentionedUsers(Collections.singletonList(tweet));
            mentioned.remove(author); // users can't follow themselves

            if (!mentioned.isEmpty()) {
                followsGraph.computeIfAbsent(author, k -> new HashSet<>())
                        .addAll(mentioned);
            }
        }
        return followsGraph;
    }

    /**
     * Find the most-followed users in a social network.
     *
     * @param followsGraph
     *            a social network (a map where keys are usernames and values
     *            are sets of usernames) representing follows relationships,
     *            not modified by this method.
     * @return a list of usernames who have at least one follower, sorted in
     *         descending order of popularity (number of followers). Users
     *         with equal follower counts appear in alphabetical order.
     */
    public static List<String> influencers(Map<String, Set<String>> followsGraph) {
        Map<String, Integer> followerCounts = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : followsGraph.entrySet()) {
            String follower = entry.getKey().toLowerCase();
            for (String followed : entry.getValue()) {
                String user = followed.toLowerCase();
                if (!user.equals(follower)) { // a self-follow is not influence
                    followerCounts.merge(user, 1, Integer::sum);
                }
            }
        }

        return followerCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(
                        Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
