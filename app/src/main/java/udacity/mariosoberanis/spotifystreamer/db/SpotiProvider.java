package udacity.mariosoberanis.spotifystreamer.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import udacity.mariosoberanis.spotifystreamer.R;
import udacity.mariosoberanis.spotifystreamer.activities.SearchActivity;
import udacity.mariosoberanis.spotifystreamer.utils.BestImage;

/*
* Spoti provider handells all the Content
*/
public class SpotiProvider extends ContentProvider {

    // Tag used when logging.
    private static final String TAG = SpotiProvider.class.getCanonicalName();

    /*
    * These are tied to the the query sArtistsByQuery in SpotiProvider.
    * If the attributes returned by that query changes, these values must be updated.
    */
    public static final int ARTISTS_BY_QUERY_QUERY_STRING = 1;
    public static final int ARTISTS_BY_QUERY_CREATE_TIME = 2;
    public static final int ARTISTS_BY_QUERY_QUERY_ID = 3;
    public static final int ARTISTS_BY_QUERY_ARTIST_ID = 4;
    public static final int ARTISTS_BY_QUERY_ID = 5;
    public static final int ARTISTS_BY_QUERY_SPOTIFY_ID = 6;
    public static final int ARTISTS_BY_QUERY_ARTIST_NAME = 7;
    public static final int ARTISTS_BY_QUERY_ARTIST_ICON = 8;
    public static final int ARTISTS_BY_QUERY_LAST_UPDATED = 9;

    /*
    * These are tied to the the query sTracksByArtist in SpotiProvider.
    * If the attributes returned by that query changes, these values must be updated.
    */
    public static final int TRACKS_BY_ARTIST_ARTIST_ID = 0;
    public static final int TRACKS_BY_ARTIST_ARTIST_SPOTIFY_ID = 1;
    public static final int TRACKS_BY_ARTIST_ARTIST_NAME = 2;
    public static final int TRACKS_BY_ARTIST_ARTIST_ICON = 3;
    public static final int TRACKS_BY_ARTIST_LAST_UPDATED_TIME = 4;
    public static final int TRACKS_BY_ARTIST_LAST_UPDATE_COUNTRY = 5;
    public static final int TRACKS_BY_ARTIST_TRACK_ID = 6;
    public static final int TRACKS_BY_ARTIST_TRACK_ARTIST_ID = 7;
    public static final int TRACKS_BY_ARTIST_TRACK_SPOTIFY_ID = 8;
    public static final int TRACKS_BY_ARTIST_TRACK_NAME = 9;
    public static final int TRACKS_BY_ARTIST_ALBUM_NAME = 10;
    public static final int TRACKS_BY_ARTIST_DURATION = 11;
    public static final int TRACKS_BY_ARTIST_EXPLICIT = 12;
    public static final int TRACKS_BY_ARTIST_PLAYABLE = 13;
    public static final int TRACKS_BY_ARTIST_POPULARITY = 14;
    public static final int TRACKS_BY_ARTIST_PREVIEW_URL = 15;
    public static final int TRACKS_BY_ARTIST_TRACK_ICON = 16;
    public static final int TRACKS_BY_ARTIST_TRACK_IMAGE = 17;

    /*
    * Indicies for the columns returned when looking for a cached Query.
    */
    private static final int QUERY_QUERY_ID = 0;

    private static final UriMatcher sUriMatcher;

    private SpotiDbHelper mDbHelper;

    /*
    * Constants used by the matcher and query method to determine
    * what type of query to execute.
    */
    static final int GET_ARTISTS = 10;
    static final int GET_TRACKS = 20;

    private static final SQLiteQueryBuilder sArtistsByQuery;
    private static final SQLiteQueryBuilder sTracksByArtist;

    private SpotifyService mSpotifyService;

    // Desired height and width for icons and images.
    private int idealIconWidth;
    private int idealIconHeight;
    private int idealImageWidth;
    private int idealImageHeight;

    // Length of time, in milliseconds, before a cached item is considered stale.
    private static final long MAX_CACHE_TIME = 1000 * 60 * 60 * 2; // 2 hours

    // Used for reading and writing dates to and from the database.  Note: all times are in UTC.
    private static final SimpleDateFormat dateFormatter =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Number of songs that will be pre fetch
    private static final int NUM_RESULTS_TO_PRE_FETCH_TRACKS = 3;

    static {

        /*
        * When reading/writing to the database, all times will be in UTC.
        */
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        sArtistsByQuery = new SQLiteQueryBuilder();

        /*
        * Joins a Query to the set of Artists that match that query using the query_artist
        * join table.
        */
        sArtistsByQuery.setTables(
                SpotiContract.QueryEntry.TABLE_NAME +
                        " INNER JOIN " +
                        SpotiContract.ArtistQuery.TABLE_NAME +
                        " ON " +
                        SpotiContract.QueryEntry.TABLE_NAME + "." + SpotiContract.QueryEntry._ID +
                        " = " +
                        SpotiContract.ArtistQuery.TABLE_NAME + "." + SpotiContract.ArtistQuery.COLUMN_QUERY_ID +
                        " INNER JOIN " +
                        SpotiContract.ArtistEntry.TABLE_NAME +
                        " ON " +
                        SpotiContract.ArtistQuery.TABLE_NAME + "." + SpotiContract.ArtistQuery.COLUMN_ARTIST_ID +
                        " = " +
                        SpotiContract.ArtistEntry.TABLE_NAME + "." + SpotiContract.ArtistEntry._ID
        );

        sTracksByArtist = new SQLiteQueryBuilder();

        /*
        * Get the tracks associated with an artist.
        */
        sTracksByArtist.setTables(
                SpotiContract.ArtistEntry.TABLE_NAME +
                        " INNER JOIN " +
                        SpotiContract.TrackEntry.TABLE_NAME +
                        " ON " +
                        SpotiContract.TrackEntry.TABLE_NAME + "." + SpotiContract.TrackEntry.COLUMN_ARTIST_ID +
                        " = " +
                        SpotiContract.ArtistEntry.TABLE_NAME + "." + SpotiContract.ArtistEntry._ID
        );

        /*
        * Create the URI matches and add it's patterns.
        */

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        // /* Mathches a String
        sUriMatcher.addURI(SpotiContract.CONTENT_AUTHORITY,
                SpotiContract.PATH_GET_ARTISTS + "/*", GET_ARTISTS);

        sUriMatcher.addURI(SpotiContract.CONTENT_AUTHORITY,
                SpotiContract.PATH_GET_TRACKS + "/*", GET_TRACKS);
    }

    @Override
    public boolean onCreate() {

        mDbHelper = new SpotiDbHelper(getContext());

        if (mSpotifyService == null) {
            mSpotifyService = new SpotifyApi().getService();
        }

        idealIconWidth = (int) getContext().getResources().getDimension(R.dimen.icon_width);
        idealIconHeight = (int) getContext().getResources().getDimension(R.dimen.icon_height);

        idealImageWidth = (int) getContext().getResources().getDimension(R.dimen.track_image_download_width);
        idealImageHeight = (int) getContext().getResources().getDimension(R.dimen.track_image_download_height);

        return true;
    }

    /*
    * Allows us to override the SpotiService for testing.
    */
    void setSpotiService(SpotifyService service)  {
        mSpotifyService = service;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        Date cacheCutOffTime = new Date(System.currentTimeMillis() - MAX_CACHE_TIME);

        Cursor retCursor;

        switch (sUriMatcher.match(uri)) {

            case GET_ARTISTS: {
                retCursor = getArtists(uri, db, cacheCutOffTime);
                break;
            }
            case GET_TRACKS: {
                retCursor = getTracks(uri, db, cacheCutOffTime);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (retCursor != null) retCursor.setNotificationUri(getContext().getContentResolver(), uri);

        return retCursor;
    }

    /*
    * This method is pretty long, but it handles all the work needed to get the set of artists
    * that match the query given in the URI.  It implements a cache so that artists may be
    * retrieved from the SQLite database instead of via a network call.
    */


    private Cursor getArtists(Uri uri, SQLiteDatabase db, Date cacheCutOffTime) {

        Log.d(TAG, "getArtists() called. uri:" + uri.toString());

        final Pager<Artist> pager;

        final String query = uri.getLastPathSegment().toLowerCase().trim();

        /*
        * Does a record exist in the Query table matching this request?
        */
        Cursor checkForCachedQueryCursor = db.query(
                SpotiContract.QueryEntry.TABLE_NAME,
                null,
                SpotiContract.QueryEntry.COLUMN_QUERY + " = ?",
                new String[]{query},
                null,
                null,
                null);

        if (checkForCachedQueryCursor.moveToFirst()) {

            Log.d(TAG, "Record exists in Query table for query: " + query);

            /*
            * Has the Query record expired?
            */
            Date queryCreateTime;

            try {
                String queryCreateTimeStr =
                        checkForCachedQueryCursor.getString(checkForCachedQueryCursor.getColumnIndex(
                                SpotiContract.QueryEntry.COLUMN_CREATE_TIME));

                queryCreateTime = dateFormatter.parse(queryCreateTimeStr);

            } catch (ParseException e) {

                Log.e(TAG, "Error parsing create date for query: " + query);

                /*
                * If there's an error parsing the time stamp, this will default
                * to refreshing the data.
                */
                queryCreateTime = null;
            }

            // Finished query of Query table.
            checkForCachedQueryCursor.close();

            if ((queryCreateTime != null) && (queryCreateTime.after(cacheCutOffTime))) {
                Log.d(TAG, "Cache hit for query: " + query);

                // Fetch Artists matching the query.
                return sArtistsByQuery.query(
                        db,
                        null,
                        SpotiContract.QueryEntry.COLUMN_QUERY + " = ?",
                        new String[]{query},
                        null,
                        null,
                        null);

            } else {

                Log.d(TAG, "Cache stale for query:" + query);

                try {

                    /*
                    * Query Spotify for Matching Artists.
                    */
                    ArtistsPager artistsPager = mSpotifyService.searchArtists(query);
                    pager = artistsPager.artists;
                    Log.d(TAG, "Successfully retrieved artists for query: " + query);

                    /*
                    * Delete ArtistQuery Entry.
                    */
                    int queryId = checkForCachedQueryCursor.getInt(QUERY_QUERY_ID);

                    db.delete(
                            SpotiContract.ArtistQuery.TABLE_NAME,
                            SpotiContract.ArtistQuery.COLUMN_QUERY_ID + " = ?",
                            new String[]{"" + queryId}
                    );

                    /*
                    * Delete Query Record.
                    */
                    db.delete(
                            SpotiContract.QueryEntry.TABLE_NAME,
                            SpotiContract.QueryEntry._ID + " = ?",
                            new String[]{"" + queryId}
                    );

                } catch (Exception e) {

                    /*
                    * Error, Using Stale Data.
                    */
                    Log.e(TAG, "Failed to fetch data for query: " + query + " Using stale data");
                    Log.e(TAG, "Error: " + e.getMessage());

                    return sArtistsByQuery.query(
                            db,
                            null,
                            SpotiContract.QueryEntry.COLUMN_QUERY + " = ?",
                            new String[]{query},
                            null,
                            null,
                            null);
                }
            }

        } else {

            Log.d(TAG, "Record does not exist in Query table for query: " + query);

            checkForCachedQueryCursor.close();

            try {
                ArtistsPager artistPager = mSpotifyService.searchArtists(query);
                pager = artistPager.artists;
                Log.d(TAG, "Successfully retrieved artists for query: " + query);

            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch data for query: " + query);
                Log.e(TAG, "Error: " + e.getMessage());

                // Fetch failed - show error
                return null;

            }
        }

        /*
        * Create new Query Record.
        */
        ContentValues values = new ContentValues();
        values.put(SpotiContract.QueryEntry.COLUMN_QUERY, query);
        values.put(SpotiContract.QueryEntry.COLUMN_CREATE_TIME, dateFormatter.format(new Date()));
        long queryRecordId = db.insert(SpotiContract.QueryEntry.TABLE_NAME, null, values);

        if (queryRecordId < 0) {
            throw new android.database.SQLException("Error creating query record. query:" + query);
        }

       /*
       * For Each Returned Artist
       */
        int n = 0;

        for (Artist artist : pager.items) {
            long artistRecordId = createArtistRecord(db, artist);

            /*
            * Create ArtistQuery Record.  This is a many-to-many join between
            * Queries and Artists.
            */
            values = new ContentValues();
            values.put(SpotiContract.ArtistQuery.COLUMN_QUERY_ID, queryRecordId);
            values.put(SpotiContract.ArtistQuery.COLUMN_ARTIST_ID, artistRecordId);

            if (db.insert(SpotiContract.ArtistQuery.TABLE_NAME, null, values) < 0) {

                throw new android.database.SQLException(
                        "Error creating query_artist record. queryRecordId:" + queryRecordId +
                                " artistRecordId:" + artistRecordId);
            }

            /*
            * Fetch track data for the top n artists matching the query.
            */
            if (n < NUM_RESULTS_TO_PRE_FETCH_TRACKS) {
                getTracks(artistRecordId, artist.id, db, cacheCutOffTime);
                n++;
            }

        }

        flushArtistCache(db, cacheCutOffTime);

        /*
        * Return List of Artists.
        */
        return sArtistsByQuery.query(
                db,
                null,
                SpotiContract.QueryEntry.TABLE_NAME + "." + SpotiContract.QueryEntry.COLUMN_QUERY + " = ?",
                new String[]{query},
                null,
                null,
                null);

    }

    private long createArtistRecord(SQLiteDatabase db, Artist artist) {

        /*
        * Create / Update Artist data.
        * Note, we don't try to perform an Update, always insert.  The table definition
        * has a unique constraint on the Artist table such that if a duplicate
        * spotify id is entered, the existing record will be replaced.
        */
        ContentValues values = new ContentValues();
        values.put(SpotiContract.ArtistEntry.COLUMN_SPOTIFY_ID, artist.id);
        values.put(SpotiContract.ArtistEntry.COLUMN_NAME, artist.name);

        Image i = BestImage.getClosestImageSize(artist.images, idealIconWidth,
                idealIconHeight);

        values.put(SpotiContract.ArtistEntry.COLUMN_ICON, (i == null) ? null : i.url);

        long artistRecordId = db.insert(SpotiContract.ArtistEntry.TABLE_NAME, null, values);

        if (artistRecordId < 0) {
            throw new android.database.SQLException(
                    "Error creating Artist record.  artistId:" + artist.id);
        }
        return artistRecordId;
    }

    /*
    * Removes all queries that are older than cacheCutOffTime.
    */
    private void flushArtistCache(SQLiteDatabase db, Date cacheCutOffTime) {

        /*
        * Begin cleaning up expired records in the cache.
        */
        String cacheCutOffTimeStr = dateFormatter.format(cacheCutOffTime);

        db.beginTransaction();

        try {

            /*
            * Delete ArtistQuery records where the related Query has expired.
            */

            final String FLUSH_ARTIST_QUERY_WHERE_CLAUSE =
                    SpotiContract.ArtistQuery.COLUMN_QUERY_ID + " IN (SELECT " +
                            SpotiContract.QueryEntry._ID + " FROM " +
                            SpotiContract.QueryEntry.TABLE_NAME + " WHERE " +
                            SpotiContract.QueryEntry.COLUMN_CREATE_TIME + " < ?)";

            db.delete(
                    SpotiContract.ArtistQuery.TABLE_NAME,
                    FLUSH_ARTIST_QUERY_WHERE_CLAUSE,
                    new String[]{cacheCutOffTimeStr}
            );

            /*
            * Delete Query records that have expired.
            */
            final String FLUSH_QUERY_WHERE_CLAUSE =
                    SpotiContract.QueryEntry.COLUMN_CREATE_TIME + " < ?";

            db.delete(
                    SpotiContract.QueryEntry.TABLE_NAME,
                    FLUSH_QUERY_WHERE_CLAUSE,
                    new String[]{cacheCutOffTimeStr}
            );

            /*
            * Delete Tracks for all Artists that no longer have entries in ArtistQuery.
            */
            final String FLUSH_TRACKS_WHERE_CLAUSE =
                    SpotiContract.TrackEntry.COLUMN_ARTIST_ID + " IN (SELECT " +
                            SpotiContract.ArtistEntry._ID + " FROM " +
                            SpotiContract.ArtistEntry.TABLE_NAME + " WHERE " +
                            SpotiContract.ArtistEntry._ID + " NOT IN (SELECT " +
                            SpotiContract.ArtistQuery.COLUMN_ARTIST_ID + " FROM " +
                            SpotiContract.ArtistQuery.TABLE_NAME + "))";

            db.delete(
                    SpotiContract.TrackEntry.TABLE_NAME,
                    FLUSH_TRACKS_WHERE_CLAUSE,
                    null
            );

            /*
            * Delete Artists that no longer have entries in ArtistQuery.
            */
            final String FLUSH_ARTIST_WHERE_CLAUSE =
                    SpotiContract.ArtistEntry._ID + " NOT IN (SELECT " +
                            SpotiContract.ArtistQuery.COLUMN_ARTIST_ID + " FROM " +
                            SpotiContract.ArtistQuery.TABLE_NAME + ")";

            db.delete(
                    SpotiContract.ArtistEntry.TABLE_NAME,
                    FLUSH_ARTIST_WHERE_CLAUSE,
                    null
            );

            db.setTransactionSuccessful();

        } finally {

            db.endTransaction();
        }
    }

    private long fetchAndCreateArtist(String artistSpotifyId, SQLiteDatabase db) {

        try {
            Artist artist = mSpotifyService.getArtist(artistSpotifyId);
            Log.d(TAG, "Successfully retrieved artist: " + artistSpotifyId);
            long id = createArtistRecord(db, artist);
            return id;

        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch data for artist: " + artistSpotifyId);
            Log.e(TAG, "Error: " + e.getMessage());

            return 0;

        }
    }

    /*
    * Gets tracks for the given artist where the artist's spotify ID is the last
    * segment of the given Uri.
    * This uses the artist's spotify ID to look up their record in the database
    * so that getTracks(long, String, SQLiteDatabase, Date) can be used.
    */
    private Cursor getTracks(Uri uri, SQLiteDatabase db, Date cacheCutOffTime) {

        final String spotifyId = uri.getLastPathSegment();

        long artistId;

        Cursor artistLookupCursor = null;

        try {
            artistLookupCursor = db.query(
                    SpotiContract.ArtistEntry.TABLE_NAME,
                    new String[]{SpotiContract.ArtistEntry._ID},
                    SpotiContract.ArtistEntry.COLUMN_SPOTIFY_ID + " = ?",
                    new String[]{spotifyId},
                    null,
                    null,
                    null);

            /*
            * The artist record may be gone, so query Spotify and re-create it.
            * This can happen if the cache was very recently flushed.
            */

            if (artistLookupCursor.moveToFirst()) {
                artistId = artistLookupCursor.getInt(
                        artistLookupCursor.getColumnIndex(SpotiContract.ArtistEntry._ID));

            } else {
                artistId = fetchAndCreateArtist(spotifyId, db);
            }

        } finally {
            if (artistLookupCursor != null) artistLookupCursor.close();
        }

        return getTracks(artistId, spotifyId, db, cacheCutOffTime);

    }

    /*
    * Gets tracks for the given artistID where artistId is a key into the Artist table
    * in the given database.
    */
    private Cursor getTracks(long artistId, String artistSpotifyId, SQLiteDatabase db,
                             Date cacheCutOffTime) {

        Log.d(TAG, "getTracks() called. artistId:" + artistId);

        // Get the user's country code, and store it in a map.
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        String countryCode = settings.getString(SearchActivity.PREF_COUNTRY_CODE, "MX").toUpperCase();

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put(SpotifyService.COUNTRY, countryCode);

        // Note if explicit tracks are allowed
        boolean allowExplicit = settings.getBoolean(SearchActivity.PREF_ALLOW_EXPLICIT, true);

        /*
        * Get the Artists last tracks updated time.  Ensure that is was for our
        * current country.
        */
        final String ARTISTS_CACHE_WHERE_CLAUSE =
                SpotiContract.ArtistEntry._ID + " = ?" +
                        " AND " +
                        SpotiContract.ArtistEntry.COLUMN_LAST_UPDATE_COUNTRY + " = ?";

        Cursor artistLastUpdateTimeCursor = db.query(
                SpotiContract.ArtistEntry.TABLE_NAME,
                new String[]{SpotiContract.ArtistEntry.COLUMN_TRACKS_LAST_UPDATED},
                ARTISTS_CACHE_WHERE_CLAUSE,
                new String[]{"" + artistId, countryCode},
                null,
                null,
                null);

        Date tracksLastUpdated = null;

        if (artistLastUpdateTimeCursor.moveToFirst()) {

            String tracksLastUpdatedStr = artistLastUpdateTimeCursor.getString(
                    artistLastUpdateTimeCursor.getColumnIndex(
                            SpotiContract.ArtistEntry.COLUMN_TRACKS_LAST_UPDATED));

            try {
                if (tracksLastUpdatedStr != null) {
                    tracksLastUpdated = dateFormatter.parse(tracksLastUpdatedStr);
                }
            } catch (ParseException e) {
                tracksLastUpdated = null;
            }
        }

        artistLastUpdateTimeCursor.close();

        List<Track> topTracks = null;
        
        /*
        * Is Artist's tracksUpdate NULL?
        */
        if (tracksLastUpdated == null) {

            try {

                /*
                * Fetch Tracks from Spotify
                */
                Tracks result = mSpotifyService.getArtistTopTrack(artistSpotifyId, locationMap);
                if (result != null) topTracks = result.tracks;
                Log.d(TAG, "Successfully retrieved top tracks for artist: " + artistSpotifyId);

            } catch (Exception e) {
                
                /*
                * Error
                */
                Log.e(TAG, "Failed to fetch to tracks for artist: " + artistSpotifyId);
                Log.e(TAG, "Error: " + e.getMessage());
                return null;
            }

        } else {

            /*
            * Artist's last Track update expired ?
            */
            if (tracksLastUpdated.before(cacheCutOffTime)) {

                try {

                    /*
                    * Fetch Tracks from Spotify
                    */
                    Tracks result = mSpotifyService.getArtistTopTrack(artistSpotifyId, locationMap);
                    if (result != null) topTracks = result.tracks;

                    Log.d(TAG, "Successfully retrieved top tracks for artist: " + artistSpotifyId);

                    /*
                    * Delete Existing Tracks for Artist.
                    */
                    db.delete(
                            SpotiContract.TrackEntry.TABLE_NAME,
                            SpotiContract.TrackEntry.COLUMN_ARTIST_ID + " = ?",
                            new String[]{"" + artistId}
                    );

                } catch (Exception e) {

                    /*
                    * Error, use Stale Data
                    */
                    Log.e(TAG, "Failed to fetch to tracks for artist: " +
                            artistSpotifyId + ", using Stale Data.");

                    Log.e(TAG, "Error: " + e.getMessage());

                    String whereClause = generateTracksByArtistWhereClause(allowExplicit);

                    return sTracksByArtist.query(
                            db,
                            null,
                            whereClause,
                            new String[]{"" + artistId, countryCode},
                            null,
                            null,
                            null);
                }

            } else {

                /*
                * Return cached list of tracks.
                */

                String whereClause = generateTracksByArtistWhereClause(allowExplicit);

                return sTracksByArtist.query(
                        db,
                        null,
                        whereClause,
                        new String[]{"" + artistId, countryCode},
                        null,
                        null,
                        null);

            }

        }

        for (Track track : topTracks) {

             /*
            * Create Track data.
            */
            ContentValues values = new ContentValues();
            values.put(SpotiContract.TrackEntry.COLUMN_SPOTIFY_ID, track.id);
            values.put(SpotiContract.TrackEntry.COLUMN_ARTIST_ID, artistId);
            values.put(SpotiContract.TrackEntry.COLUMN_TITLE, track.name);
            values.put(SpotiContract.TrackEntry.COLUMN_ALBUM, track.album.name);
            values.put(SpotiContract.TrackEntry.COLUMN_DURATION, track.duration_ms);
            values.put(SpotiContract.TrackEntry.COLUMN_EXPLICIT, track.explicit);

            // Sometimes is_playable is null.  In this case, I've found that it is playable.
            values.put(SpotiContract.TrackEntry.COLUMN_PLAYABLE,
                    ((track.is_playable == null) || (track.is_playable)));

            values.put(SpotiContract.TrackEntry.COLUMN_POPULARITY, track.popularity);
            values.put(SpotiContract.TrackEntry.COLUMN_PREVIEW, track.preview_url);

            Image i = BestImage.getClosestImageSize(track.album.images, idealIconWidth,
                    idealIconHeight);

            values.put(SpotiContract.TrackEntry.COLUMN_ICON, (i == null) ? null : i.url);

            i = BestImage.getClosestImageSize(track.album.images, idealImageWidth, idealImageHeight);

            values.put(SpotiContract.TrackEntry.COLUMN_IMAGE, (i == null) ? null : i.url);

            if (db.insert(SpotiContract.TrackEntry.TABLE_NAME, null, values) < 0) {
                throw new android.database.SQLException("Error creating track record.  trackId:" + track.id);
            }

        }

        /*
        * Set Artist's track last update timestamp.
        */
        ContentValues values = new ContentValues();
        values.put(SpotiContract.ArtistEntry.COLUMN_TRACKS_LAST_UPDATED, dateFormatter.format(new Date()));
        values.put(SpotiContract.ArtistEntry.COLUMN_LAST_UPDATE_COUNTRY, countryCode);

        db.update(
                SpotiContract.ArtistEntry.TABLE_NAME,
                values,
                SpotiContract.ArtistEntry._ID + " = ?",
                new String[]{"" + artistId}
        );

        /*
        * Begin removing expired tracks from cache.
        */
        flushTrackCache(db, cacheCutOffTime);

        /*
        * Return tracks for Artist.
        */
        return sTracksByArtist.query(
                db,
                null,
                SpotiContract.ArtistEntry.TABLE_NAME + "." + SpotiContract.ArtistEntry._ID + " = ?",
                new String[]{"" + artistId},
                null,
                null,
                null);

    }

    private String generateTracksByArtistWhereClause(boolean allowExplicit) {

        final String TRACKS_BY_ARTIST_WHERE_CLAUSE =
                SpotiContract.ArtistEntry.TABLE_NAME + "." +
                        SpotiContract.ArtistEntry._ID + " = ?" +
                        " AND " +
                        SpotiContract.ArtistEntry.TABLE_NAME + "." +
                        SpotiContract.ArtistEntry.COLUMN_LAST_UPDATE_COUNTRY + " = ?";

        final String TRACK_BY_ARTIST_NO_EXPLICIT_WHERE_CLAUSE =
                TRACKS_BY_ARTIST_WHERE_CLAUSE +
                        " AND " +
                        SpotiContract.TrackEntry.TABLE_NAME + "." +
                        SpotiContract.TrackEntry.COLUMN_EXPLICIT + " = 0";

        return (allowExplicit) ? TRACKS_BY_ARTIST_WHERE_CLAUSE :
                TRACK_BY_ARTIST_NO_EXPLICIT_WHERE_CLAUSE;

    }

    /*
    * Removes Track entries for all Artists where Artist.tracksLastUpdate is older than
    * cacheCutOffTime.
    */
    private void flushTrackCache(SQLiteDatabase db, Date cacheCutOffTime) {

        /*
        * Begin cleaning up expired tracks in the cache.
        */
        String cacheCutOffTimeStr = dateFormatter.format(cacheCutOffTime);

        db.beginTransaction();

        try {

            /*
            * Set the Artist's trackLastUpdated to NULL, if it has expired
            */
            ContentValues values = new ContentValues();
            values.putNull(SpotiContract.ArtistEntry.COLUMN_TRACKS_LAST_UPDATED);

            db.update(
                    SpotiContract.ArtistEntry.TABLE_NAME,
                    values,
                    SpotiContract.ArtistEntry.COLUMN_TRACKS_LAST_UPDATED + " < ?",
                    new String[]{cacheCutOffTimeStr}
            );

            /*
            * Delete all tracks where the related Artist's trackLastUpdate is NULL.
            */
            db.delete(
                    SpotiContract.TrackEntry.TABLE_NAME,
                    SpotiContract.TrackEntry.COLUMN_ARTIST_ID + " IN (SELECT " +
                            SpotiContract.ArtistEntry._ID + " FROM " +
                            SpotiContract.ArtistEntry.TABLE_NAME + " WHERE " +
                            SpotiContract.ArtistEntry.COLUMN_TRACKS_LAST_UPDATED +
                            " IS NULL)",
                    null

            );

            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.e(TAG, "Error querying for artists with expired caches: " + e.getMessage());

        } finally {
            db.endTransaction();
        }
    }

    @Override
    public String getType(Uri uri) {

        final int match = sUriMatcher.match(uri);

        switch (match) {
            case GET_ARTISTS:
                return SpotiContract.GET_ARTISTS_CONTENT_TYPE;

            case GET_TRACKS:
                return SpotiContract.GET_TRACKS_CONTENT_TYPE;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

    }

    /*
    * Insert operation is not currently requirerd.
    */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Direct Inserts Unsupported uri: " + uri);
    }

    /*
    * This should be called when the user's Preferences change.  If the country code,
    * or allow explicit settings change, the cache data wouldn't be affected.  So it's
    * a good idea to flush the cache at that point and start fresh.
    */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsDeleted = 0;

        try {
            db.beginTransaction();

            rowsDeleted += db.delete(SpotiContract.TrackEntry.TABLE_NAME, null, null);
            rowsDeleted += db.delete(SpotiContract.ArtistQuery.TABLE_NAME, null, null);
            rowsDeleted += db.delete(SpotiContract.QueryEntry.TABLE_NAME, null, null);
            rowsDeleted += db.delete(SpotiContract.ArtistEntry.TABLE_NAME, null, null);

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }

        return rowsDeleted;
    }

    /*
    * Update operation is not currently supported.
    */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Direct Updates Unsupported uri: " + uri);
    }

}
