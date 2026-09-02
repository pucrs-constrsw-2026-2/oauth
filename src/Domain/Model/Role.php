<?php

declare(strict_types=1);

namespace App\Domain\Model;

enum Role: string
{
    case ADMINISTRATOR = 'administrator';
    case COORDINATOR = 'coordinator';
    case PROFESSOR = 'professor';
    case STUDENT = 'student';

    public static function isValid(string $role): bool
    {
        return self::tryFrom($role) !== null;
    }
}
