FROM php:8.2-fpm-alpine

# Instala ferramentas de sistema e dependências
RUN apk add --no-cache \
    bash \
    curl \
    git \
    unzip \
    zip \
    libzip-dev \
    icu-dev

# Instala extensões PHP
RUN docker-php-ext-configure intl \
    && docker-php-ext-install -j$(nproc) intl opcache

# Copia Composer do container oficial
COPY --from=composer:2 /usr/bin/composer /usr/bin/composer

WORKDIR /var/www/html

EXPOSE 9000
CMD ["php-fpm"]
