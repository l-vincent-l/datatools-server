package com.conveyal.datatools.manager.jobs;

import com.conveyal.datatools.common.status.MonitorableJob;
import com.conveyal.datatools.manager.DataManager;
import com.conveyal.datatools.manager.models.FeedVersion;
import com.conveyal.r5.transit.TransportNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by landon on 10/11/16.
 */
public class ReadTransportNetworkJob extends MonitorableJob {
    private static final Logger LOG = LoggerFactory.getLogger(ReadTransportNetworkJob.class);
    public FeedVersion feedVersion;
    public TransportNetwork result;
    public Status status;

    public ReadTransportNetworkJob (FeedVersion feedVersion, String owner) {
        super(owner, "Reading in Transport Network for " + feedVersion.getFeedSource().name, JobType.BUILD_TRANSPORT_NETWORK);
        this.feedVersion = feedVersion;
        this.result = null;
        this.status = new Status();
        status.message = "Waiting to begin job...";
    }

    @Override
    public void run() {
        LOG.info("Reading network");
        File is;
        is = feedVersion.getTransportNetworkPath();
        try {
            feedVersion.transportNetwork = TransportNetwork.read(is);
            // check to see if distance tables are built yet... should be removed once better caching strategy is implemeneted.
            if (feedVersion.transportNetwork.transitLayer.stopToVertexDistanceTables == null) {
                feedVersion.transportNetwork.transitLayer.buildDistanceTables(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        synchronized (status) {
            status.message = "Transport network read successfully!";
            status.percentComplete = 100;
            status.completed = true;
        }
        jobFinished();
    }

    @Override
    public Status getStatus() {
        synchronized (status) {
            return status.clone();
        }
    }

    @Override
    public void handleStatusEvent(Map statusMap) {
        try {
            synchronized (status) {
                status.message = (String) statusMap.get("message");
                status.percentComplete = (double) statusMap.get("percentComplete");
                status.error = (boolean) statusMap.get("error");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void handleStatusEvent(StatusEvent statusEvent) {
//        synchronized (status) {
//            status.message = statusEvent.message;
//            status.percentComplete = statusEvent.percentComplete
//            status.error = statusEvent.error;
//        }
//    }

}
