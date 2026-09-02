<?php

declare(strict_types=1);

namespace App\Domain\Model;

final class User
{
    public function __construct(
        private ?string $id,
        private string $username,
        private string $email,
        private string $firstName,
        private string $lastName,
        private bool $enabled = true,
        private array $roles = []
    ) {
    }

    public function getId(): ?string
    {
        return $this->id;
    }

    public function getUsername(): string
    {
        return $this->username;
    }

    public function getEmail(): string
    {
        return $this->email;
    }

    public function getFirstName(): string
    {
        return $this->firstName;
    }

    public function getLastName(): string
    {
        return $this->lastName;
    }

    public function isEnabled(): bool
    {
        return $this->enabled;
    }

    public function getRoles(): array
    {
        return $this->roles;
    }

    public function toArray(): array
    {
        return [
            'id' => $this->id,
            'username' => $this->username,
            'email' => $this->email,
            'firstName' => $this->firstName,
            'lastName' => $this->lastName,
            'enabled' => $this->enabled,
            'roles' => $this->roles,
        ];
    }
}
