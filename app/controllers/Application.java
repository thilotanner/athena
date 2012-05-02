package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;
import search.BulkIndexJob;

public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void elasticsearch()
    {
        render();
    }

    public static void indexDocuments()
    {
        BulkIndexJob bulkIndexJob = new BulkIndexJob();
        bulkIndexJob.now();
        index();
    }
}