package controllers;


import play.mvc.Controller;
import search.BulkIndexJob;

public class Search extends Controller
{

    public static void index()
    {
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