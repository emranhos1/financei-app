package com.finance.service;

import com.finance.entity.TransferType;
import com.finance.repository.TransferTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferTypeService {
    private final TransferTypeRepository transferTypeRepository;

    public TransferType createTransferType(Long userId, String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Transfer type name cannot be empty");
        String trimmed = name.trim();
        if (transferTypeRepository.existsByUserIdAndNameIgnoreCase(userId, trimmed))
            throw new IllegalArgumentException("Transfer type '" + trimmed + "' already exists");
        return transferTypeRepository.save(TransferType.builder().userId(userId).name(trimmed).build());
    }

    public List<TransferType> getTransferTypesByUserId(Long userId) {
        return transferTypeRepository.findByUserId(userId);
    }

    public TransferType getTransferTypeById(Long typeId) {
        return transferTypeRepository.findById(typeId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer type not found"));
    }

    public TransferType updateTransferType(Long typeId, Long userId, String newName) {
        TransferType type = transferTypeRepository.findById(typeId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer type not found"));
        if (!type.getUserId().equals(userId)) throw new SecurityException("Access denied");

        String trimmed = newName.trim();
        boolean duplicateExists = transferTypeRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, trimmed, typeId);
        if (duplicateExists)
            throw new IllegalArgumentException("Transfer type '" + trimmed + "' already exists");

        type.setName(trimmed);
        return transferTypeRepository.save(type);
    }

    public void deleteTransferType(Long typeId, Long userId) {
        TransferType type = transferTypeRepository.findById(typeId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer type not found"));
        if (!type.getUserId().equals(userId)) throw new SecurityException("Access denied");
        transferTypeRepository.deleteById(typeId);
    }
}