<?php

declare(strict_types=1);

namespace App\Application\DTO\User;

final class UpdateUserDTO
{
    public function __construct(
        public readonly ?string $firstName = null,
        public readonly ?string $lastName = null,
        public readonly ?string $email = null
    ) {
    }

    public static function fromArray(array $data): self
    {
        return new self(
            firstName: isset($data['firstName']) ? (string) $data['firstName'] : (isset($data['first_name']) ? (string) $data['first_name'] : null),
            lastName: isset($data['lastName']) ? (string) $data['lastName'] : (isset($data['last_name']) ? (string) $data['last_name'] : null),
            email: isset($data['email']) ? (string) $data['email'] : null
        );
    }
}
