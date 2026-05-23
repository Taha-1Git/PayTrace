package com.paytrace.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.paytrace.dao.UserConnectionDAO;
import com.paytrace.dao.UserDAO;
import com.paytrace.dao.VendorDAO;
import com.paytrace.models.AdministratorUser;
import com.paytrace.models.RegularUser;
import com.paytrace.models.Session;
import com.paytrace.models.User;
import com.paytrace.models.UserConnection;
import com.paytrace.models.Vendor;
import com.paytrace.models.enums.AccountType;
import com.paytrace.models.enums.TargetType;
import com.paytrace.utils.CodeGenerator;

import java.time.LocalDateTime;
import java.util.Optional;

public class AuthService {

    private final UserDAO           userDAO   = new UserDAO();
    private final VendorDAO         vendorDAO = new VendorDAO();
    private final UserConnectionDAO connDAO   = new UserConnectionDAO();

    // ─────────────────────────────────────────────────────
    //  LOGIN
    // ─────────────────────────────────────────────────────
    public Session loginAdmin(String email, String password) throws Exception {
        User u = authenticate(email, password, AccountType.ADMIN);
        return buildSession(u, null, null, "System");
    }
    public Session loginAdministrator(String email, String password, String ownerCode)
            throws Exception {
        User u = authenticate(email, password, AccountType.ADMINISTRATOR);

        // Find the vendor this administrator owns
        Optional<Vendor> v = vendorDAO.findByAdministratorId(u.getUserId());
        if (v.isEmpty()) {
            throw new RuntimeException(
                    "Administrator owns no vendor. Please re-register and create one.");
        }
        if (!v.get().getAccessCode().equals(ownerCode)) {
            throw new RuntimeException("Invalid vendor code for this administrator.");
        }

        // Tag the user with the vendor they manage (used by canAccessVendor())
        if (u instanceof AdministratorUser) {
            ((AdministratorUser) u).setManagedVendorId(v.get().getVendorId());
        }

        return buildSession(u, TargetType.VENDOR,
                v.get().getVendorId(), v.get().getVendorName());
    }
    public Session loginUser(String email, String password, String connectionCode)
            throws Exception {
        User u = authenticate(email, password, AccountType.USER);

        // Blank code = user lands on the Choose-Access screen to pick or request
        if (connectionCode == null || connectionCode.trim().isEmpty()) {
            return buildSession(u, null, null, "No access selected");
        }

        String code = connectionCode.trim();
        if (code.length() != 4) {
            throw new RuntimeException("Code must be 4 digits (or leave blank).");
        }

        Optional<UserConnection> conn =
                connDAO.findByUserAndCode(u.getUserId(), code);
        if (conn.isEmpty()) {
            throw new RuntimeException(
                    "Code not recognised. Leave code blank to request access first.");
        }

        // Lookup vendor name (target_type is always VENDOR in v6+)
        String name = vendorDAO.findById(conn.get().getTargetId())
                .map(Vendor::getVendorName)
                .orElse("Vendor");

        // Tag the regular user with the vendor context they're entering
        if (u instanceof RegularUser) {
            ((RegularUser) u).setCurrentVendorContext(conn.get().getTargetId());
        }

        return buildSession(u, TargetType.VENDOR,
                conn.get().getTargetId(), name);
    }

    // ─────────────────────────────────────────────────────
    //  Internal — shared authentication
    // ─────────────────────────────────────────────────────

    private User authenticate(String email, String password, AccountType expected)
            throws Exception {
        if (email == null || email.isBlank() || password == null || password.isBlank())
            throw new RuntimeException("Email and password are required.");

        Optional<User> ou = userDAO.findByEmail(email.trim().toLowerCase());
        if (ou.isEmpty()) throw new RuntimeException("Invalid email or password.");

        User u = ou.get();
        if (!u.isActive()) throw new RuntimeException("Account is inactive.");
        if (u.isBlocked()) {
            throw new RuntimeException("Your account has been blocked. Reason: "
                    + (u.getBlockedReason() == null ? "(unspecified)" : u.getBlockedReason()));
        }
        if (u.getAccountType() != expected) {
            throw new RuntimeException("Wrong login type for this account.");
        }

        BCrypt.Result r = BCrypt.verifyer().verify(password.toCharArray(), u.getPasswordHash());
        if (!r.verified) {
            userDAO.incrementFailedAttempts(u.getUserId());
            throw new RuntimeException("Invalid email or password.");
        }
        userDAO.resetFailedAttempts(u.getUserId());
        return u;
    }

    private Session buildSession(User u, TargetType ctxType, String ctxId, String ctxName) {
        Session s = new Session();
        s.setSessionId(java.util.UUID.randomUUID().toString());
        s.setUserId(u.getUserId());
        s.setRole(u.getAccountType());
        s.setLoginTime(LocalDateTime.now());
        s.setActive(true);
        s.setIpAddress("localhost");
        s.setContextType(ctxType);
        s.setContextId(ctxId);
        s.setContextName(ctxName);
        return s;
    }

    // ─────────────────────────────────────────────────────
    //  REGISTER
    // ─────────────────────────────────────────────────────
    public void registerAdministratorWithVendor(String fullName, String email, String password,
                                                String vendorName, String vendorEmail,
                                                String vendorAddress) throws Exception {
        validateRegister(fullName, email, password);
        if (vendorName == null || vendorName.isBlank())
            throw new RuntimeException("Vendor name is required.");

        AdministratorUser u = new AdministratorUser();
        u.setFullName(fullName.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setPasswordHash(hash(password));
        userDAO.save(u);

        User saved = userDAO.findByEmail(u.getEmail())
                .orElseThrow(() -> new RuntimeException("Failed to load saved user."));

        Vendor v = new Vendor();
        v.setVendorName(vendorName.trim());
        v.setVendorEmail(vendorEmail);
        v.setVendorAddress(vendorAddress);
        v.setAccessCode(CodeGenerator.generateUnique4DigitCode());
        v.setAccountNumber(CodeGenerator.generateUniqueAccountNumber());
        v.setAdministratorId(saved.getUserId());
        vendorDAO.save(v);
    }
    public void registerUser(String fullName, String email, String password) throws Exception {
        validateRegister(fullName, email, password);
        RegularUser u = new RegularUser();
        u.setFullName(fullName.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setPasswordHash(hash(password));
        userDAO.save(u);
    }

    private void validateRegister(String fullName, String email, String password) {
        if (fullName == null || fullName.isBlank())
            throw new RuntimeException("Full name required.");
        if (email == null || email.isBlank())
            throw new RuntimeException("Email required.");
        if (!email.contains("@"))
            throw new RuntimeException("Invalid email.");
        if (password == null || password.length() < 8)
            throw new RuntimeException("Password must be at least 8 characters.");
        try {
            if (userDAO.findByEmail(email.trim().toLowerCase()).isPresent())
                throw new RuntimeException("This email is already registered.");
        } catch (Exception ignored) {}
    }

    private String hash(String pwd) {
        return BCrypt.withDefaults().hashToString(10, pwd.toCharArray());
    }
    public void logout(String sessionId) {
        // Sessions are in-memory only for now; nothing to do.
    }
}