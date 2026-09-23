package by.vstu.zamok.user.controller;

import by.vstu.zamok.user.dto.AddressDto;
import by.vstu.zamok.user.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','MANAGER','ADMIN')")
    public ResponseEntity<List<AddressDto>> list(JwtAuthenticationToken authentication) {
        String keycloakId = authentication.getToken().getSubject();
        return ResponseEntity.ok(addressService.listMyAddresses(keycloakId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MANAGER','ADMIN')")
    public ResponseEntity<AddressDto> create(@RequestBody @Valid AddressDto dto, JwtAuthenticationToken authentication) {
        String keycloakId = authentication.getToken().getSubject();
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.addMyAddress(keycloakId, dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MANAGER','ADMIN')")
    public ResponseEntity<AddressDto> update(@PathVariable("id") Long id,
                                             @RequestBody @Valid AddressDto dto,
                                             JwtAuthenticationToken authentication) {
        String keycloakId = authentication.getToken().getSubject();
        return ResponseEntity.ok(addressService.updateMyAddress(keycloakId, id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MANAGER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, JwtAuthenticationToken authentication) {
        String keycloakId = authentication.getToken().getSubject();
        addressService.deleteMyAddress(keycloakId, id);
        return ResponseEntity.noContent().build();
    }
}
