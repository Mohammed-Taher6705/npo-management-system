package com.npo.user;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.npo.common.DatabaseConnection;
import com.npo.beneficiary.Need;

public class Beneficiary extends User{
    private String address;
    private int familySize;
    private List<Need> needs = new ArrayList<>();

     public Beneficiary() {
        role = "BENEFICIARY";
    }

    public Beneficiary(String name, String email, String phone, String password,
                       String preferredChannel, String address, int familySize) {
        super(name, email, phone, password, preferredChannel, "BENEFICIARY");
        this.address = address;
        this.familySize = familySize;
    }

    public void addNeed(Need n) {
        needs.add(n);
        edit();
        if (n.isUrgent()) {
            n.announceUrgent(this);
        }
    }

    public void removeNeed(Need n) {
        for (int i = 0; i < needs.size(); i++) {
            if (needs.get(i).getId().equals(n.getId())) {
                needs.remove(i);
                break;
            }
        }
        edit();
    }

    public List<Need> getNeeds() {
        return needs;
    }

    public String getAddress() {
        return address;
    }

    public int getFamilySize() {
        return familySize;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setFamilySize(int familySize) {
        this.familySize = familySize;
    }

    private void saveBeneficiaryFields() {
        List<String> needIds = new ArrayList<>();
        for (Need n : needs) {
            needIds.add(n.getId());
        }
        Document data = new Document("address", address)
                .append("familySize", familySize)
                .append("needIds", needIds);
        DatabaseConnection.getInstance().update("users", id, data);
    }

    @Override
    public void create() {
        super.create();
        saveBeneficiaryFields();
    }

    @Override
    public void edit() {
        super.edit();
        saveBeneficiaryFields();
    }

    @Override
    public Beneficiary read(String id) {
        if (super.read(id) == null) {
            return null;
        }
        Document doc = DatabaseConnection.getInstance().selectById("users", id);
        address = doc.getString("address");
        familySize = doc.getInteger("familySize");

        needs = new ArrayList<>();
        List<String> needIds = doc.getList("needIds", String.class);
        for (String needId : needIds) {
            Need n = new Need();
            n.read(needId);
            needs.add(n);
        }
        return this;
    }

    @Override
    public boolean login(String email, String password) {
        if (!super.login(email, password)) {
            return false;
        }
        read(id);
        return true;
    }

}