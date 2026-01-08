package com.mahal.service;

import com.mahal.database.CertificateDAO;
import java.time.LocalDate;

public class CertificateNumberService {
    private final CertificateDAO certificateDAO;

    public CertificateNumberService() {
        this.certificateDAO = new CertificateDAO();
    }

    public String generateMarriageCertificateNumber() {
        String prefix = "MC";
        String year = String.valueOf(LocalDate.now().getYear());
        long count = certificateDAO.countByType("Marriage") + 1;
        return String.format("%s-%s-%05d", prefix, year, count);
    }

    public String generateJamathCertificateNumber() {
        String prefix = "JC";
        String year = String.valueOf(LocalDate.now().getYear());
        long count = certificateDAO.countByType("Jamath") + 1;
        return String.format("%s-%s-%05d", prefix, year, count);
    }

    public String generateDeathCertificateNumber() {
        String prefix = "DC";
        String year = String.valueOf(LocalDate.now().getYear());
        long count = certificateDAO.countByType("Death") + 1;
        return String.format("%s-%s-%05d", prefix, year, count);
    }

    public String generateCustomCertificateNumber() {
        String prefix = "CC";
        String year = String.valueOf(LocalDate.now().getYear());
        long count = certificateDAO.countByType("Custom") + 1;
        return String.format("%s-%s-%05d", prefix, year, count);
    }
}




