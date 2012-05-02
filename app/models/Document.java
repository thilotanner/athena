package models;

import play.db.jpa.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
public class Document extends Model
{
    @Temporal(TemporalType.TIMESTAMP)
    public Date date;

    @Column(length = 2048)
    public String title;

    @Lob
    public String text;
}
