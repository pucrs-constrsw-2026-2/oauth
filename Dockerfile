FROM php:8.2-cli-alpine

# Instala ferramentas de sistema, extensões e nodejs (exigido pelo healthcheck do compose raiz)
RUN apk add --no-cache \
    bash \
    curl \
    git \
    unzip \
    zip \
    libzip-dev \
    icu-dev \
    nodejs

# Instala extensões PHP
RUN docker-php-ext-configure intl \
    && docker-php-ext-install -j$(nproc) intl opcache

# Copia Composer do container oficial
COPY --from=composer:2 /usr/bin/composer /usr/bin/composer

WORKDIR /var/www/html

# Copia código-fonte da aplicação
COPY . /var/www/html

# Instala dependências PHP
RUN composer install --optimize-autoloader --no-interaction --no-progress

# Habilita múltiplos workers no built-in web server do PHP
ENV PHP_CLI_SERVER_WORKERS=4

# Porta interna padrão (3001 definida no compose da raiz)
EXPOSE 3001

# Inicia o servidor HTTP embutido do PHP apontando para o front controller Symfony
CMD ["sh", "-c", "exec php -S 0.0.0.0:${OAUTH_INTERNAL_API_PORT:-3001} -t public public/index.php"]
