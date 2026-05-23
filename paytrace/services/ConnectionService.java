package com.paytrace.services;

import com.paytrace.dao.ConnectionRequestDAO;
import com.paytrace.dao.UserConnectionDAO;
import com.paytrace.dao.VendorDAO;
import com.paytrace.models.ConnectionRequest;
import com.paytrace.models.UserConnection;
import com.paytrace.models.Vendor;
import com.paytrace.models.enums.RequestStatus;
import com.paytrace.models.enums.TargetType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service-layer for User → Vendor connection requests.
 * The User submits a request to access a vendor's data; the Administrator
 * who owns that vendor reviews and approves/rejects.
 */
public class ConnectionService {

    private final ConnectionRequestDAO reqDAO    = new ConnectionRequestDAO();
    private final UserConnectionDAO    connDAO   = new UserConnectionDAO();
    private final VendorDAO            vendorDAO = new VendorDAO();
    public void submitRequest(String userId, TargetType type, String targetId,
                              String address, String job, String phone,
                              String extraInfo, String accountNumber) throws Exception {
        ConnectionRequest r = new ConnectionRequest();
        r.setUserId(userId);
        r.setTargetType(TargetType.VENDOR); // multivendor system: target is always a vendor
        r.setTargetId(targetId);
        r.setUserAddress(address);
        r.setUserJob(job);
        r.setUserPhone(phone);
        r.setExtraInfo(extraInfo);
        r.setUserAccountNumber(accountNumber);
        r.setStatus(RequestStatus.PENDING);
        reqDAO.save(r);
    }
    public List<ConnectionRequest> findPendingForAdministrator(String adminId)
            throws Exception {
        Optional<Vendor> v = vendorDAO.findByAdministratorId(adminId);
        if (v.isPresent()) {
            return filterPending(reqDAO.findByTarget(v.get().getVendorId()));
        }
        return new ArrayList<>();
    }

    private List<ConnectionRequest> filterPending(List<ConnectionRequest> all) {
        List<ConnectionRequest> pending = new ArrayList<>();
        for (ConnectionRequest r : all)
            if (r.getStatus() == RequestStatus.PENDING) pending.add(r);
        return pending;
    }
    public String approveRequest(String requestId, String adminId) throws Exception {
        ConnectionRequest match = null;
        for (ConnectionRequest r : findPendingForAdministrator(adminId)) {
            if (r.getRequestId().equals(requestId)) { match = r; break; }
        }
        if (match == null)
            throw new RuntimeException("Request not found or already decided.");

        // Get the 4-digit code for the target vendor
        String code = vendorDAO.findById(match.getTargetId())
                .map(Vendor::getAccessCode)
                .orElseThrow(() -> new RuntimeException("Vendor not found."));

        // Create the user_connection row if not already present
        if (connDAO.findByUserAndCode(match.getUserId(), code).isEmpty()) {
            UserConnection uc = new UserConnection();
            uc.setUserId(match.getUserId());
            uc.setTargetType(match.getTargetType());
            uc.setTargetId(match.getTargetId());
            uc.setAccessCode(code);
            uc.setUserAccountNumber(match.getUserAccountNumber());
            connDAO.save(uc);
        }

        reqDAO.updateStatus(requestId, RequestStatus.APPROVED, adminId);
        return code;
    }
    public void rejectRequest(String requestId, String adminId) throws Exception {
        reqDAO.updateStatus(requestId, RequestStatus.REJECTED, adminId);
    }
}
