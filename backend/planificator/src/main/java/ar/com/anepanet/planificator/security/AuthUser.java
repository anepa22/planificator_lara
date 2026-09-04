package ar.com.anepanet.planificator.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

public class AuthUser implements UserDetails {

    private final UUID id;
    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean mustChangePassword;

    public AuthUser(UUID id, String username, Collection<? extends GrantedAuthority> authorities) {
        this(id, username, authorities, false);
    }

    public AuthUser(
            UUID id,
            String username,
            Collection<? extends GrantedAuthority> authorities,
            boolean mustChangePassword) {
        this.id = id;
        this.username = username;
        this.authorities = authorities;
        this.mustChangePassword = mustChangePassword;
    }

    public UUID getId() {
        return id;
    }

    public boolean mustChangePassword() {
        return mustChangePassword;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
